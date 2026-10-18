package com.romankozak.forwardappmobile.features.mainscreen.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLinkedAppearanceMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
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


    @Test
    fun `No beacon CUT atomically retires exact occurrences and clears associations`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "a-owner", "b-owner")
                val placements = CanonicalHierarchyPlacementRepository(db)

                val firstId = placements.createPrimaryAppearance(
                    target("a-owner"),
                    now = 10L,
                )
                val secondId = placements.createPrimaryAppearance(
                    target("b-owner"),
                    now = 11L,
                )

                val selected = mapOf(
                    "a-owner" to requireNotNull(
                        placements.getPlacement(firstId),
                    ).toHierarchyOccurrenceRef(),
                    "b-owner" to requireNotNull(
                        placements.getPlacement(secondId),
                    ).toHierarchyOccurrenceRef(),
                )

                assertEquals(
                    2,
                    db.mainBeaconDao().getAllWorkspaceCrossRefsSync().size,
                )

                val moved = repository(db).moveContextsToNoBeaconWithOccurrences(
                    selected,
                )

                assertEquals(2, moved)
                assertTrue(
                    db.mainBeaconDao().getAllWorkspaceCrossRefsSync().isEmpty(),
                )
                assertNull(placements.getPlacement(firstId))
                assertNull(placements.getPlacement(secondId))
                assertTrue(retiredMainBeaconTableAbsentForCut(db))
            } finally {
                db.close()
            }
        }

    @Test
    fun `No beacon late Beacon DAO failure rolls back H1 retirement and provenance`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "a-owner", "b-owner")
                val placements = CanonicalHierarchyPlacementRepository(db)

                // Keep PRIMARY identities live. CUT selects exact LINK
                // appearances so linked-provenance rollback is observable.
                placements.createPrimaryAppearance(
                    target("a-owner"),
                    now = 10L,
                )
                placements.createPrimaryAppearance(
                    target("b-owner"),
                    now = 11L,
                )

                val firstId = placements.createLinkAppearance(
                    target = target("a-owner"),
                    now = 12L,
                )
                val secondId = placements.createLinkAppearance(
                    target = target("b-owner"),
                    now = 13L,
                )

                // PlacementKind.LINK does not imply linked-presentation
                // provenance. Establish both sparse sidecars explicitly.
                val linkedMutation =
                    HierarchyPlacementLinkedAppearanceMutationCoordinator(db)

                linkedMutation.setLinkedAppearance(
                    placementId = firstId,
                    now = 14L,
                )
                linkedMutation.setLinkedAppearance(
                    placementId = secondId,
                    now = 15L,
                )

                val selected = mapOf(
                    "a-owner" to requireNotNull(
                        placements.getPlacement(firstId),
                    ).toHierarchyOccurrenceRef(),
                    "b-owner" to requireNotNull(
                        placements.getPlacement(secondId),
                    ).toHierarchyOccurrenceRef(),
                )

                val selectedIds = listOf(firstId, secondId)
                val linkedDao = db.hierarchyPlacementLinkedAppearanceDao()
                val scopeDao = db.hierarchyPlacementGroupScopeDao()

                val beforeRefs =
                    db.mainBeaconDao().getAllWorkspaceCrossRefsSync()
                val beforePlacements =
                    db.hierarchyPlacementDao().getAll()
                val beforePresentation =
                    BigPicturePresentationRepository(db).snapshot()
                val beforeLinked = selectedIds.map { id ->
                    linkedDao.getByPlacementId(id.value)
                }
                val beforeScopes = selectedIds.map { id ->
                    scopeDao.getByPlacementId(id.value)
                }

                assertEquals(2, beforeRefs.size)
                assertTrue(
                    "Expected live linked-appearance provenance before CUT",
                    beforeLinked.all { it != null && !it.isDeleted },
                )

                // H1 retirement runs first. The following DELETE is forced
                // to fail, proving rollback across the outer Room boundary.
                db.openHelper.writableDatabase.execSQL(
                    """
                    CREATE TRIGGER cc2c3b_late_crossref_abort
                    BEFORE DELETE ON main_beacon_workspace_cross_ref
                    BEGIN
                        SELECT RAISE(
                            ABORT,
                            'cc2c3b_deliberate_late_delete_failure'
                        );
                    END
                    """.trimIndent(),
                )

                val failure = requireNotNull(
                    runCatching {
                        repository(db).moveContextsToNoBeaconWithOccurrences(
                            selected,
                        )
                    }.exceptionOrNull(),
                ) {
                    "Expected late SQLite DELETE rejection"
                }

                val diagnostic = generateSequence(failure) { it.cause }
                    .mapNotNull { it.message }
                    .joinToString(" | ")

                assertTrue(
                    "Expected late DAO trigger failure, got: $diagnostic",
                    diagnostic.contains(
                        "cc2c3b_deliberate_late_delete_failure",
                    ),
                )

                assertEquals(
                    beforeRefs,
                    db.mainBeaconDao().getAllWorkspaceCrossRefsSync(),
                )
                assertEquals(
                    beforePlacements,
                    db.hierarchyPlacementDao().getAll(),
                )
                assertEquals(
                    beforePresentation,
                    BigPicturePresentationRepository(db).snapshot(),
                )
                assertEquals(
                    beforeLinked,
                    selectedIds.map { id ->
                        linkedDao.getByPlacementId(id.value)
                    },
                )
                assertEquals(
                    beforeScopes,
                    selectedIds.map { id ->
                        scopeDao.getByPlacementId(id.value)
                    },
                )

                selectedIds.forEach { id ->
                    assertNotNull(
                        "Selected occurrence must remain live: ${id.value}",
                        placements.getPlacement(id),
                    )
                }

                assertTrue(retiredMainBeaconTableAbsentForCut(db))
            } finally {
                db.close()
            }
        }


    @Test
    fun `Beacon CUT rejects typed container child and rolls back associations`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "a-owner")
                val placements = CanonicalHierarchyPlacementRepository(db)
                val h1 = H1StructuralMutationCoordinator(db)

                val selectedId = placements.createPrimaryAppearance(
                    target("a-owner"),
                    now = 10L,
                )

                h1.createContainer(
                    title = "Child section",
                    destination = H1StructuralParent.Node(
                        H1TypedNode.Occurrence(selectedId.value),
                    ),
                    id = "cc2c4-beacon-child-container",
                    now = 20L,
                )

                // Capture identity only after container creation, so any
                // legitimate structural version change is represented.
                val selected = requireNotNull(
                    placements.getPlacement(selectedId),
                ).toHierarchyOccurrenceRef()

                val beforeRefs =
                    db.mainBeaconDao().getAllWorkspaceCrossRefsSync()
                val beforePlacements =
                    db.hierarchyPlacementDao().getAll()
                val beforePresentation =
                    BigPicturePresentationRepository(db).snapshot()

                val failure = runCatching {
                    repository(db).moveRelatedContextsToBeaconWithOccurrences(
                        beaconId = "destination",
                        occurrencesByContextId = mapOf(
                            "a-owner" to selected,
                        ),
                    )
                }.exceptionOrNull()

                assertTrue(
                    "Expected H1 typed child-policy rejection: $failure",
                    failure is HierarchyChildPolicyRejectedException,
                )

                assertEquals(
                    beforeRefs,
                    db.mainBeaconDao().getAllWorkspaceCrossRefsSync(),
                )
                assertEquals(
                    beforePlacements,
                    db.hierarchyPlacementDao().getAll(),
                )
                assertEquals(
                    beforePresentation,
                    BigPicturePresentationRepository(db).snapshot(),
                )
                assertNotNull(placements.getPlacement(selectedId))
                assertTrue(retiredMainBeaconTableAbsentForCut(db))
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
            h1StructuralMutationCoordinator = com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator(db),
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
