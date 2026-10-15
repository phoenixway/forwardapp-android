package com.romankozak.forwardappmobile.features.mainscreen.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementGroupScopeMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.orientation.MainBeaconOrientationBridge
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainBeaconWorkspaceCutRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `successful CUT moves association and retires exact occurrence`() = runBlocking {
        val db = database()
        try {
            seed(db, "a-owner")
            val placements = CanonicalHierarchyPlacementRepository(db)
            val selectedId = placements.createPrimaryAppearance(target("a-owner"), now = 10L)
            val selected = requireNotNull(placements.getPlacement(selectedId))
                .toHierarchyOccurrenceRef()

            val moved = repository(db).moveRelatedContextsToBeaconWithOccurrences(
                beaconId = "destination",
                occurrencesByContextId = mapOf("a-owner" to selected),
            )

            assertEquals(1, moved)
            assertEquals(
                listOf(MainBeaconWorkspaceCrossRef("destination", "a-owner", 0L)),
                db.mainBeaconDao().getAllWorkspaceCrossRefsSync(),
            )
            assertTrue(retiredMainBeaconTableAbsentForCut(db))
            assertNull(placements.getPlacement(selectedId))
        } finally {
            db.close()
        }
    }

    @Test
    fun `stale occurrence rejects CUT leaving associations and placements unchanged`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "a-owner")
                val placements = CanonicalHierarchyPlacementRepository(db)
                val selectedId = placements.createPrimaryAppearance(target("a-owner"), now = 10L)
                val original = requireNotNull(placements.getPlacement(selectedId))
                    .toHierarchyOccurrenceRef()

                val beforeRefs = db.mainBeaconDao().getAllWorkspaceCrossRefsSync()
                val beforePlacements = db.hierarchyPlacementDao().getAll()

                val failure = runCatching {
                    repository(db).moveRelatedContextsToBeaconWithOccurrences(
                        beaconId = "destination",
                        occurrencesByContextId = mapOf(
                            "a-owner" to original.copy(siblingOrder = original.siblingOrder + 1L),
                        ),
                    )
                }.exceptionOrNull()

                assertNotNull(failure)
                assertEquals(beforeRefs, db.mainBeaconDao().getAllWorkspaceCrossRefsSync())
                assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
                assertTrue(retiredMainBeaconTableAbsentForCut(db))
            } finally {
                db.close()
            }
        }

    @Test
    fun `late occurrence removal failure rolls back Beacon mutation and earlier removal`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "a-owner", "b-owner", "child")
                val placements = CanonicalHierarchyPlacementRepository(db)

                val firstId = placements.createPrimaryAppearance(target("a-owner"), now = 10L)
                val secondId = placements.createPrimaryAppearance(target("b-owner"), now = 11L)
                placements.createPrimaryAppearance(
                    target = target("child"),
                    parentPlacementId = secondId,
                    now = 12L,
                )

                val selected = mapOf(
                    "a-owner" to requireNotNull(placements.getPlacement(firstId))
                        .toHierarchyOccurrenceRef(),
                    "b-owner" to requireNotNull(placements.getPlacement(secondId))
                        .toHierarchyOccurrenceRef(),
                )

                val beforeRefs = db.mainBeaconDao().getAllWorkspaceCrossRefsSync()
                assertTrue(retiredMainBeaconTableAbsentForCut(db))
                val beforePlacements = db.hierarchyPlacementDao().getAll()

                val failure = runCatching {
                    repository(db).moveRelatedContextsToBeaconWithOccurrences(
                        beaconId = "destination",
                        occurrencesByContextId = selected,
                    )
                }.exceptionOrNull()

                assertNotNull("Expected child-policy rejection", failure)
                assertEquals(beforeRefs, db.mainBeaconDao().getAllWorkspaceCrossRefsSync())
                assertTrue(retiredMainBeaconTableAbsentForCut(db))
                assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
                assertNotNull(placements.getPlacement(firstId))
                assertNotNull(placements.getPlacement(secondId))
            } finally {
                db.close()
            }
        }

    private suspend fun seed(db: AppDatabase, vararg ownerIds: String) {
        db.mainBeaconDao().insertBeacon(MainBeacon(id = "source", title = "Source"))
        db.mainBeaconDao().insertBeacon(MainBeacon(id = "destination", title = "Destination"))

        db.workspaceDao().upsert(ownerIds.map(::workspace))

        ownerIds.filterNot { it == "child" }.forEachIndexed { index, ownerId ->
            db.mainBeaconDao().insertWorkspaceCrossRefs(
                listOf(MainBeaconWorkspaceCrossRef("source", ownerId, index.toLong())),
            )
        }
    }

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.STANDALONE.name,
            sourceContextId = null,
        )

    private fun target(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun repository(db: AppDatabase) =
        MainBeaconRepository(
            appDatabase = db,
            mainBeaconDao = db.mainBeaconDao(),
            orientationBridge = mockk<MainBeaconOrientationBridge>(relaxed = true),
            hierarchyPlacementRepository = CanonicalHierarchyPlacementRepository(db),
            groupScopeCoordinator = HierarchyPlacementGroupScopeMutationCoordinator(db),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}

private fun retiredMainBeaconTableAbsentForCut(database: com.romankozak.forwardappmobile.database.AppDatabase): Boolean =
    database.openHelper.writableDatabase.query(
        "SELECT COUNT(*) FROM sqlite_master " +
            "WHERE type = 'table' AND name = 'main_beacon_context_cross_ref'",
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0) == 0L
    }
