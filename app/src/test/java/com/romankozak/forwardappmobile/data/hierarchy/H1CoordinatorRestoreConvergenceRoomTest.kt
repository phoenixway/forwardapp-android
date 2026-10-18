package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.hierarchy.container.projectH1UnifiedStructuralGraph
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * CC.9: exact historical occurrence Restore through the production H1
 * typed structural authority, with schema 199 and real Room transactions.
 *
 * Restore is not a create: placement identity, old membership history,
 * provenance correlation and semantic-target admission must survive.
 */
@RunWith(RobolectricTestRunner::class)
class H1CoordinatorRestoreConvergenceRoomTest {
    private fun database() =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private suspend fun workspace(db: AppDatabase, id: String) {
        db.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id, id, null, null, 1L, 1L, null, false, 1L,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private fun target(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private suspend fun primary(db: AppDatabase, id: String): PlacementId {
        workspace(db, id)
        return CanonicalHierarchyPlacementRepository(db)
            .createPrimaryAppearance(target(id), now = 10L)
    }

    private suspend fun link(db: AppDatabase, id: String): PlacementId {
        workspace(db, id)
        return CanonicalHierarchyPlacementRepository(db)
            .createLinkAppearance(target(id), now = 10L)
    }

    @Test
    fun `restore preserves exact placement and correlated linked provenance`(): Unit =
        runBlocking {
            val db = database()
            try {
                val placement = link(db, "linked")
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val writer = H1StructuralMutationCoordinator(db)

                linked.setLinkedAppearance(placement, now = 90L)
                writer.removeOccurrence(placement, now = 20L)

                val retired = requireNotNull(
                    db.hierarchyPlacementDao().getById(placement.value),
                )
                val retiredSidecar = requireNotNull(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(placement.value),
                )
                assertThat(retired.isDeleted).isTrue()
                assertThat(retiredSidecar.isDeleted).isTrue()
                assertThat(retired.updatedAt).isEqualTo(retiredSidecar.updatedAt)

                writer.restoreOccurrence(placement, now = 120L)

                val restored = requireNotNull(
                    db.hierarchyPlacementDao().getById(placement.value),
                )
                val restoredSidecar = requireNotNull(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(placement.value),
                )
                assertThat(restored.id).isEqualTo(placement.value)
                assertThat(restored.isDeleted).isFalse()
                assertThat(restored.version).isEqualTo(retired.version + 1L)
                assertThat(restored.updatedAt).isGreaterThan(retired.updatedAt)
                assertThat(restored.syncedAt).isNull()
                assertThat(restoredSidecar.isDeleted).isFalse()
                assertThat(restoredSidecar.version)
                    .isEqualTo(retiredSidecar.version + 1L)
                assertThat(restoredSidecar.updatedAt)
                    .isEqualTo(restored.updatedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `independently retired linked provenance stays retired`(): Unit =
        runBlocking {
            val db = database()
            try {
                val placement = link(db, "independent")
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val writer = H1StructuralMutationCoordinator(db)

                linked.setLinkedAppearance(placement, now = 20L)
                linked.retireLinkedAppearance(placement, now = 30L)
                val independentTombstone = requireNotNull(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(placement.value),
                )

                writer.removeOccurrence(placement, now = 40L)
                writer.restoreOccurrence(placement, now = 50L)

                assertThat(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(placement.value),
                ).isEqualTo(independentTombstone)
                assertThat(
                    db.hierarchyPlacementDao().getById(placement.value)!!.isDeleted,
                ).isFalse()
            } finally {
                db.close()
            }
        }

    @Test
    fun `PRIMARY conflict rejects restore with no state mutation`(): Unit =
        runBlocking {
            val db = database()
            try {
                val original = primary(db, "conflict")
                val writer = H1StructuralMutationCoordinator(db)
                writer.removeOccurrence(original, now = 20L)

                val competing = CanonicalHierarchyPlacementRepository(db)
                    .createPrimaryAppearance(target("conflict"), now = 30L)
                val beforeRows = db.hierarchyPlacementDao().getAll()
                val beforePresentation = BigPicturePresentationRepository(db).snapshot()

                val failure = runCatching {
                    writer.restoreOccurrence(original, now = 40L)
                }.exceptionOrNull()

                assertThat(failure)
                    .isInstanceOf(PrimaryAppearanceConflictException::class.java)
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforeRows)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(beforePresentation)
                assertThat(
                    CanonicalHierarchyPlacementRepository(db)
                        .getPrimaryAppearance(target("conflict"))!!.id,
                ).isEqualTo(competing)
            } finally {
                db.close()
            }
        }

    @Test
    fun `deleted semantic target rejects restore atomically`(): Unit =
        runBlocking {
            val db = database()
            try {
                val placement = primary(db, "deleted-target")
                val writer = H1StructuralMutationCoordinator(db)
                writer.removeOccurrence(placement, now = 20L)

                val targetRow = requireNotNull(
                    db.workspaceDao().getById("deleted-target"),
                )
                db.workspaceDao().upsert(
                    listOf(
                        targetRow.copy(
                            isDeleted = true,
                            updatedAt = 30L,
                            version = targetRow.version + 1L,
                            syncedAt = null,
                        ),
                    ),
                )

                val before = db.hierarchyPlacementDao().getAll()
                val failure = runCatching {
                    writer.restoreOccurrence(placement, now = 40L)
                }.exceptionOrNull()

                assertThat(failure)
                    .isInstanceOf(HierarchyTargetDeletedException::class.java)
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(before)
            } finally {
                db.close()
            }
        }

    @Test
    fun `historical Policy B membership remains inert after restore`(): Unit =
        runBlocking {
            val db = database()
            try {
                val placement = primary(db, "historical-member")
                val presentation = BigPicturePresentationRepository(db)
                val writer = H1StructuralMutationCoordinator(db)

                val container = presentation.create(
                    title = "Retired section",
                    now = 20L,
                    id = "historical-policy-b-container",
                )
                presentation.assign(
                    placementId = placement.value,
                    containerId = container,
                    now = 21L,
                )
                presentation.tombstone(
                    id = container,
                    expectedVersion = 1L,
                    now = 22L,
                )

                // Deliberately reconstruct a valid historical transport state:
                // retired occurrence and retained live membership pointing to
                // a deleted container. Policy B treats that edge as inert.
                val live = requireNotNull(
                    db.hierarchyPlacementDao().getById(placement.value),
                )
                db.hierarchyPlacementDao().upsertAll(
                    listOf(
                        live.copy(
                            isDeleted = true,
                            version = live.version + 1L,
                            updatedAt = 30L,
                            syncedAt = null,
                        ),
                    ),
                )

                val historical = presentation.snapshot()
                val historicalMembership = historical.memberships.single()
                assertThat(historicalMembership.isDeleted).isFalse()
                assertThat(
                    historical.containers.single().isDeleted,
                ).isTrue()

                writer.restoreOccurrence(placement, now = 40L)

                val restored = requireNotNull(
                    db.hierarchyPlacementDao().getById(placement.value),
                )
                assertThat(restored.isDeleted).isFalse()
                assertThat(restored.id).isEqualTo(placement.value)
                assertThat(restored.version).isEqualTo(live.version + 2L)
                assertThat(restored.syncedAt).isNull()
                assertThat(presentation.snapshot()).isEqualTo(historical)

                val graph = projectH1UnifiedStructuralGraph(
                    db.hierarchyPlacementDao().getAll().map {
                        it.toHierarchyPlacementStrict()
                    },
                    presentation.snapshot(),
                )
                assertThat(
                    graph.parentOf(H1TypedNode.Occurrence(placement.value)),
                ).isNull()
            } finally {
                db.close()
            }
        }

    @Test
    fun `restore under retired parent is rejected without partial write`(): Unit =
        runBlocking {
            val db = database()
            try {
                val parent = primary(db, "parent")
                workspace(db, "child")
                val child = CanonicalHierarchyPlacementRepository(db)
                    .createPrimaryAppearance(
                        target = target("child"),
                        parentPlacementId = parent,
                        now = 11L,
                    )
                val writer = H1StructuralMutationCoordinator(db)

                writer.removeOccurrenceSubtree(parent, now = 20L)
                val before = db.hierarchyPlacementDao().getAll()
                val failure = runCatching {
                    writer.restoreOccurrence(child, now = 30L)
                }.exceptionOrNull()

                assertThat(failure)
                    .isInstanceOf(HierarchyParentDeletedException::class.java)
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(before)

                writer.restoreOccurrence(parent, now = 40L)
                writer.restoreOccurrence(child, now = 41L)
                assertThat(
                    db.hierarchyPlacementDao().getById(child.value)!!.isDeleted,
                ).isFalse()
            } finally {
                db.close()
            }
        }

    @Test
    fun `outer semantic transaction rollback reverses coordinator restore`(): Unit =
        runBlocking {
            val db = database()
            try {
                val placement = link(db, "outer-rollback")
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val writer = H1StructuralMutationCoordinator(db)

                linked.setLinkedAppearance(placement, now = 11L)
                writer.removeOccurrence(placement, now = 20L)

                val beforePlacement = requireNotNull(
                    db.hierarchyPlacementDao().getById(placement.value),
                )
                val beforeLinked = requireNotNull(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(placement.value),
                )

                val failure = runCatching {
                    db.withTransaction {
                        writer.restoreOccurrence(placement, now = 30L)
                        error("forced outer semantic transaction rollback")
                    }
                }.exceptionOrNull()

                assertThat(failure).isInstanceOf(IllegalStateException::class.java)
                assertThat(db.hierarchyPlacementDao().getById(placement.value))
                    .isEqualTo(beforePlacement)
                assertThat(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(placement.value),
                ).isEqualTo(beforeLinked)
            } finally {
                db.close()
            }
        }
}
