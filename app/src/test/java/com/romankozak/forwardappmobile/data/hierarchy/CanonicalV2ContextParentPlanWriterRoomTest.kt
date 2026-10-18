package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.projectH1UnifiedStructuralGraph
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalV2ContextParentPlanWriterRoomTest {
    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private data class Fixture(
        val source: PlacementId,
        val destination: PlacementId,
        val primary: PlacementId,
        val selectedLink: PlacementId,
    )

    private fun target(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private suspend fun fixture(db: AppDatabase): Fixture {
        db.workspaceDao().upsert(
            listOf("ctx-source", "ctx-dest", "ctx-child").map { id ->
                WorkspaceEntity(
                    id, id, null, null, 1, 1, null, false, 1,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                )
            },
        )

        val h1 = CanonicalHierarchyPlacementRepository(db)
        val source = h1.createPrimaryAppearance(target("ctx-source"), now = 1)
        val destination = h1.createPrimaryAppearance(target("ctx-dest"), now = 2)
        val primary = h1.createPrimaryAppearance(
            target("ctx-child"),
            parentPlacementId = source,
            now = 3,
        )
        val selectedLink = h1.createLinkAppearance(
            target("ctx-child"),
            parentPlacementId = source,
            now = 4,
        )
        return Fixture(source, destination, primary, selectedLink)
    }

    private fun plan(f: Fixture) = HierarchyActionPlan(
        structural = listOf(
            HierarchyStructuralOperand.RemoveOccurrence(f.selectedLink),
            HierarchyStructuralOperand.MoveOccurrence(
                placementId = f.primary,
                newParentPlacementId = f.destination,
            ),
            HierarchyStructuralOperand.CreateAppearance(
                target = target("ctx-child"),
                parentPlacementId = f.destination,
                placementKind = PlacementKind.LINK,
            ),
        ),
        semantic = listOf(
            HierarchySemanticOperand.EnsureDirectionFrontLinkIfEnabled(
                parentWorkspaceTarget = target("ctx-dest"),
                childWorkspaceTarget = target("ctx-child"),
            ),
        ),
    )

    private fun writer(
        db: AppDatabase,
        contextRepository: ContextRepository,
    ) = CanonicalV2ContextParentPlanWriter(
        database = db,
        coordinator = com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator(db),
        contextRepository = contextRepository,
    )

    private suspend fun assertValid(db: AppDatabase) {
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll().map {
                it.toHierarchyPlacementStrict()
            },
            BigPicturePresentationRepository(db).snapshot(),
        )
    }

    @Test
    fun `real fused transaction removes moves creates then invokes semantic companion`() =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val semantic = mockk<ContextRepository>()
                coEvery {
                    semantic.ensureDirectionFrontLinkIfEnabled(
                        "ctx-dest", "ctx-child",
                    )
                } just Runs

                val before = db.hierarchyPlacementDao().getAll()
                writer(db, semantic).execute(plan(f), now = 20)

                val after = db.hierarchyPlacementDao().getAll()
                assertThat(after.size).isEqualTo(before.size + 1)

                val retired = requireNotNull(
                    db.hierarchyPlacementDao().getById(f.selectedLink.value),
                )
                assertThat(retired.isDeleted).isTrue()

                val moved = requireNotNull(
                    db.hierarchyPlacementDao().getById(f.primary.value),
                )
                assertThat(moved.isDeleted).isFalse()
                assertThat(moved.parentPlacementId)
                    .isEqualTo(f.destination.value)

                val oldIds = before.map { it.id }.toSet()
                val created = after.single { it.id !in oldIds }
                assertThat(created.parentPlacementId)
                    .isEqualTo(f.destination.value)
                assertThat(created.placementKind)
                    .isEqualTo(PlacementKind.LINK.name)
                assertThat(created.isDeleted).isFalse()

                coVerify(exactly = 1) {
                    semantic.ensureDirectionFrontLinkIfEnabled(
                        "ctx-dest", "ctx-child",
                    )
                }
                assertValid(db)
            } finally {
                db.close()
            }
        }

    @Test
    fun `late semantic companion failure rolls back all structural operands`() =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val semantic = mockk<ContextRepository>()
                coEvery {
                    semantic.ensureDirectionFrontLinkIfEnabled(
                        "ctx-dest", "ctx-child",
                    )
                } throws IllegalStateException("semantic rejection")

                val before = db.hierarchyPlacementDao().getAll()
                val presentation = BigPicturePresentationRepository(db)
                val beforePresentation = presentation.snapshot()

                val failure = runCatching {
                    writer(db, semantic).execute(plan(f), now = 20)
                }.exceptionOrNull()

                assertThat(failure)
                    .isInstanceOf(IllegalStateException::class.java)
                assertThat(failure?.message)
                    .isEqualTo("semantic rejection")

                coVerify(exactly = 1) {
                    semantic.ensureDirectionFrontLinkIfEnabled(
                        "ctx-dest", "ctx-child",
                    )
                }

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(before)
                assertThat(presentation.snapshot())
                    .isEqualTo(beforePresentation)
                assertValid(db)
            } finally {
                db.close()
            }
        }

    @Test
    fun `mixed typed source supports H1 move and invokes semantic`() =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)

                // A live uncontained sibling and a generic container share
                // this source level. H1 handles both in one typed graph.
                CanonicalHierarchyPlacementRepository(db).createLinkAppearance(
                    target = target("ctx-child"),
                    parentPlacementId = f.source,
                    now = 5,
                )

                val semantic = mockk<ContextRepository>(relaxed = true)
                val presentation = BigPicturePresentationRepository(db)

                presentation.create(
                    title = "Mixed Section",
                    anchorPlacementId = f.source.value,
                    now = 10,
                    id = "ctx-mixed",
                )
                presentation.assign(
                    placementId = f.primary.value,
                    containerId = "ctx-mixed",
                    now = 11,
                )
                assertValid(db)

                val before = db.hierarchyPlacementDao().getAll()
                val beforeIds = before.map { it.id }.toSet()

                writer(db, semantic).execute(plan(f), now = 20)

                val after = db.hierarchyPlacementDao().getAll()
                assertThat(after.size).isEqualTo(before.size + 1)

                val retired = requireNotNull(
                    db.hierarchyPlacementDao().getById(f.selectedLink.value),
                )
                assertThat(retired.isDeleted).isTrue()

                val moved = requireNotNull(
                    db.hierarchyPlacementDao().getById(f.primary.value),
                )
                assertThat(moved.isDeleted).isFalse()
                assertThat(moved.parentPlacementId)
                    .isEqualTo(f.destination.value)

                val created = after.single { it.id !in beforeIds }
                assertThat(created.isDeleted).isFalse()
                assertThat(created.placementKind)
                    .isEqualTo(PlacementKind.LINK.name)
                assertThat(created.parentPlacementId)
                    .isEqualTo(f.destination.value)

                assertThat(
                    presentation.snapshot().containers.map { it.id },
                ).contains("ctx-mixed")

                coVerify(exactly = 1) {
                    semantic.ensureDirectionFrontLinkIfEnabled(
                        "ctx-dest", "ctx-child",
                    )
                }

                assertValid(db)
            } finally {
                db.close()
            }
        }

    @Test
    fun `invalid H1 destination rolls back preceding removal and skips semantic`() =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val semantic = mockk<ContextRepository>(relaxed = true)
                val presentation = BigPicturePresentationRepository(db)

                val before = db.hierarchyPlacementDao().getAll()
                val beforePresentation = presentation.snapshot()

                // The first operand retires the selected LINK.
                // The following move must reject a missing typed parent.
                val invalidPlan = plan(f).let { original ->
                    original.copy(
                        structural = original.structural.map { operand ->
                            if (operand is HierarchyStructuralOperand.MoveOccurrence) {
                                operand.copy(
                                    newParentPlacementId =
                                        PlacementId("cc2c5a-missing-destination"),
                                )
                            } else {
                                operand
                            }
                        },
                    )
                }

                val failure = runCatching {
                    writer(db, semantic).execute(invalidPlan, now = 20)
                }.exceptionOrNull()

                assertThat(failure)
                    .isInstanceOf(IllegalArgumentException::class.java)
                assertThat(failure?.message)
                    .contains("Unavailable H1 typed node")

                coVerify(exactly = 0) {
                    semantic.ensureDirectionFrontLinkIfEnabled(
                        "ctx-dest", "ctx-child",
                    )
                }

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(before)
                assertThat(presentation.snapshot())
                    .isEqualTo(beforePresentation)

                assertThat(
                    db.hierarchyPlacementDao()
                        .getById(f.selectedLink.value)
                        ?.isDeleted,
                ).isFalse()

                assertValid(db)
            } finally {
                db.close()
            }
        }

}
