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
class H1LegacyOccurrenceReorderBoundaryRoomTest {
    private fun database() =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private data class Fixture(
        val parent: PlacementId,
        val first: PlacementId,
        val second: PlacementId,
    )

    private suspend fun fixture(db: AppDatabase): Fixture {
        val names = listOf("legacy-parent", "legacy-first", "legacy-second")
        db.workspaceDao().upsert(names.map { name ->
            WorkspaceEntity(
                name, name, null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )
        })

        val repo = CanonicalHierarchyPlacementRepository(db)

        suspend fun add(name: String, parent: PlacementId?, now: Long) =
            repo.createPrimaryAppearance(
                HierarchyTargetRef(HierarchyTargetType.WORKSPACE, name),
                parentPlacementId = parent,
                now = now,
            )

        val parent = add("legacy-parent", null, 1)
        val first = add("legacy-first", parent, 2)
        val second = add("legacy-second", parent, 3)
        return Fixture(parent, first, second)
    }

    private suspend fun assertValid(db: AppDatabase) {
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll().map {
                it.toHierarchyPlacementStrict()
            },
            BigPicturePresentationRepository(db).snapshot(),
        )
    }

    @Test
    fun `legacy complete reorder remains valid for pure occurrence siblings`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val repo = CanonicalHierarchyPlacementRepository(db)

                repo.reorderSiblings(
                    parentPlacementId = f.parent,
                    orderedPlacementIds = listOf(f.second, f.first),
                    now = 20,
                )

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.second.value)!!.siblingOrder).isEqualTo(0L)
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.first.value)!!.siblingOrder).isEqualTo(1L)

                repo.reorderPlacement(
                    f.first,
                    HierarchyPlacementPosition.FIRST,
                    now = 30,
                )

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.first.value)!!.siblingOrder).isEqualTo(0L)
                assertValid(db)
            } finally {
                db.close()
            }
        }

    @Test
    fun `legacy reorder APIs reject mixed typed siblings without changing rows`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val repo = CanonicalHierarchyPlacementRepository(db)
                val presentation = BigPicturePresentationRepository(db)

                presentation.create(
                    title = "Container",
                    anchorPlacementId = f.parent.value,
                    now = 20,
                    id = "legacy-mixed-container",
                )
                presentation.assign(
                    placementId = f.first.value,
                    containerId = "legacy-mixed-container",
                    now = 21,
                )

                assertValid(db)

                val beforePlacements = db.hierarchyPlacementDao().getAll()
                val beforePresentation = presentation.snapshot()

                val completeReorder = runCatching {
                    repo.reorderSiblings(
                        parentPlacementId = f.parent,
                        orderedPlacementIds = listOf(f.second, f.first),
                        now = 30,
                    )
                }.exceptionOrNull()

                assertThat(completeReorder)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                val singleReorder = runCatching {
                    repo.reorderPlacement(
                        f.first,
                        HierarchyPlacementPosition.FIRST,
                        now = 31,
                    )
                }.exceptionOrNull()

                assertThat(singleReorder)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                val outsideReorder = runCatching {
                    repo.reorderPlacement(
                        f.second,
                        HierarchyPlacementPosition.FIRST,
                        now = 32,
                    )
                }.exceptionOrNull()

                assertThat(outsideReorder)
                    .isInstanceOf(HierarchySiblingSetMismatchException::class.java)

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforePlacements)
                assertThat(presentation.snapshot())
                    .isEqualTo(beforePresentation)

                assertValid(db)
            } finally {
                db.close()
            }
        }
}
