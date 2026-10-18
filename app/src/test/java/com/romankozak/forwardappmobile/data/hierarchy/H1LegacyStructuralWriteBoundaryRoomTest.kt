package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.projectH1UnifiedStructuralGraph
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class H1LegacyStructuralWriteBoundaryRoomTest {
    private fun database() =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private data class Fixture(
        val parent: PlacementId,
        val other: PlacementId,
        val first: PlacementId,
        val second: PlacementId,
    )

    private fun target(name: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, name)

    private suspend fun fixture(db: AppDatabase): Fixture {
        val names = listOf(
            "compat-parent", "compat-other",
            "compat-first", "compat-second",
        )
        db.workspaceDao().upsert(names.map { name ->
            WorkspaceEntity(
                name, name, null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )
        })

        val h1 = CanonicalHierarchyPlacementRepository(db)
        suspend fun add(name: String, parent: PlacementId?, now: Long) =
            h1.createPrimaryAppearance(
                target(name), parentPlacementId = parent, now = now,
            )

        val parent = add("compat-parent", null, 1)
        val other = add("compat-other", null, 2)
        val first = add("compat-first", parent, 3)
        val second = add("compat-second", parent, 4)
        return Fixture(parent, other, first, second)
    }

    private suspend fun assertGraphValid(db: AppDatabase) {
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll()
                .map { it.toHierarchyPlacementStrict() },
            BigPicturePresentationRepository(db).snapshot(),
        )
    }

    @Test
    fun `legacy move and create reject mixed source sibling level atomically`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val h1 = CanonicalHierarchyPlacementRepository(db)
                val presentation = BigPicturePresentationRepository(db)

                presentation.create(
                    "Section",
                    anchorPlacementId = f.parent.value,
                    now = 20,
                    id = "mixed-source",
                )
                presentation.assign(
                    f.first.value,
                    "mixed-source",
                    now = 21,
                )

                val beforeH1 = db.hierarchyPlacementDao().getAll()
                val beforePresentation = presentation.snapshot()

                val moveFailure = runCatching {
                    h1.movePlacement(
                        f.second, f.other, now = 30,
                    )
                }.exceptionOrNull()

                assertThat(moveFailure)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                val createFailure = runCatching {
                    h1.createLinkAppearance(
                        target("compat-first"),
                        parentPlacementId = f.parent,
                        now = 31,
                    )
                }.exceptionOrNull()

                assertThat(createFailure)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforeH1)
                assertThat(presentation.snapshot())
                    .isEqualTo(beforePresentation)

                assertGraphValid(db)
            } finally {
                db.close()
            }
        }

    @Test
    fun `legacy batch move and create reject mixed root destination`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val h1 = CanonicalHierarchyPlacementRepository(db)
                val presentation = BigPicturePresentationRepository(db)

                presentation.create(
                    "Root Section",
                    now = 20,
                    id = "mixed-root",
                )

                val beforeH1 = db.hierarchyPlacementDao().getAll()
                val beforePresentation = presentation.snapshot()

                val batchFailure = runCatching {
                    h1.movePlacements(
                        listOf(
                            HierarchyPlacementMove(
                                f.first, null,
                                HierarchyPlacementPosition.LAST,
                            ),
                            HierarchyPlacementMove(
                                f.second, null,
                                HierarchyPlacementPosition.LAST,
                            ),
                        ),
                        now = 30,
                    )
                }.exceptionOrNull()

                assertThat(batchFailure)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                val createFailure = runCatching {
                    h1.createLinkAppearance(
                        target("compat-first"),
                        parentPlacementId = null,
                        now = 31,
                    )
                }.exceptionOrNull()

                assertThat(createFailure)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforeH1)
                assertThat(presentation.snapshot())
                    .isEqualTo(beforePresentation)
                assertGraphValid(db)
            } finally {
                db.close()
            }
        }

    @Test
    fun `legacy pure sibling levels preserve move and create compatibility`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val h1 = CanonicalHierarchyPlacementRepository(db)

                h1.movePlacement(
                    f.second, f.other, now = 20,
                )

                val link = h1.createLinkAppearance(
                    target("compat-first"),
                    parentPlacementId = f.other,
                    now = 21,
                )

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.second.value)!!.parentPlacementId)
                    .isEqualTo(f.other.value)

                assertThat(db.hierarchyPlacementDao()
                    .getById(link.value)!!.parentPlacementId)
                    .isEqualTo(f.other.value)

                assertGraphValid(db)
            } finally {
                db.close()
            }
        }
}
