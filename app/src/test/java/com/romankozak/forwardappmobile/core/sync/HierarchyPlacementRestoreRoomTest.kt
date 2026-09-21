package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HierarchyPlacementRestoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `E valid full restore preserves graph ids tombstones and syncedAt and U FK check is clean`() =
        runBlocking {
            val db = database()
            try {
                val parentTarget = workspace("workspace-parent")
                val childTarget = workspace("workspace-child")
                val parent =
                    snapshot(
                        id = "parent",
                        targetId = parentTarget.id,
                        kind = "PRIMARY",
                        order = 0L,
                        syncedAt = 77L,
                        version = 3L,
                    )
                val child =
                    snapshot(
                        id = "child",
                        targetId = childTarget.id,
                        parentId = "parent",
                        kind = "LINK",
                        order = 0L,
                        syncedAt = 88L,
                        version = 4L,
                    )
                val tombstone =
                    snapshot(
                        id = "deleted",
                        targetId = childTarget.id,
                        kind = "LINK",
                        order = 1L,
                        syncedAt = 99L,
                        deleted = true,
                        version = 5L,
                    )

                restoreDataSource(db)
                    .replaceWith(
                        canonicalBundle(
                            workspaces = listOf(parentTarget, childTarget),
                            hierarchyPlacements = listOf(parent, child, tombstone),
                        ),
                    )

                val restored = db.hierarchyPlacementDao().getAll().associateBy { it.id }
                assertEquals(setOf("parent", "child", "deleted"), restored.keys)
                assertEquals("parent", restored.getValue("child").parentPlacementId)
                assertEquals(77L, restored.getValue("parent").syncedAt)
                assertEquals(88L, restored.getValue("child").syncedAt)
                assertEquals(99L, restored.getValue("deleted").syncedAt)
                assertTrue(restored.getValue("deleted").isDeleted)

                val fkViolations =
                    db.openHelper.writableDatabase
                        .query("PRAGMA foreign_key_check")
                        .use { it.count }
                assertEquals(0, fkViolations)
            } finally {
                db.close()
            }
        }

    @Test
    fun `linked appearance restore preserves exact occurrence and syncedAt`() =
        runBlocking {
            val db = database()
            try {
                val target = workspace("workspace-linked")
                val placement =
                    snapshot(
                        id = "linked-p",
                        targetId = target.id,
                        kind = "LINK",
                        syncedAt = 44L,
                        version = 3L,
                    )
                val linked =
                    linkedSnapshot(
                        placementId = placement.id,
                        syncedAt = 77L,
                        version = 4L,
                    )

                restoreDataSource(db)
                    .replaceWith(
                        canonicalBundle(
                            workspaces = listOf(target),
                            hierarchyPlacements = listOf(placement),
                            linkedAppearances = listOf(linked),
                        ),
                    )

                val restored =
                    requireNotNull(
                        db.hierarchyPlacementLinkedAppearanceDao()
                            .getByPlacementId("linked-p"),
                    )
                assertEquals("GENERAL", restored.hierarchyId)
                assertEquals(77L, restored.syncedAt)
                assertEquals(4L, restored.version)
                assertFalse(restored.isDeleted)

                val fkViolations =
                    db.openHelper.writableDatabase
                        .query("PRAGMA foreign_key_check")
                        .use { it.count }
                assertEquals(0, fkViolations)
            } finally {
                db.close()
            }
        }

    @Test
    fun `present empty linked appearance collection authoritatively clears dormant destination provenance`() =
        runBlocking {
            val db = database()
            try {
                seedPlacement(db, targetId = "workspace-1")
                seedLinkedAppearance(db, placementId = "local-p", syncedAt = 55L)
                assertNotNull(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId("local-p"),
                )

                restoreDataSource(db)
                    .replaceWith(
                        canonicalBundle(
                            workspaces = listOf(workspace("workspace-1")),
                            hierarchyPlacements =
                                listOf(
                                    snapshot(
                                        id = "local-p",
                                        targetId = "workspace-1",
                                    ),
                                ),
                            linkedAppearances = emptyList(),
                        ),
                    )

                assertTrue(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getAll()
                        .isEmpty(),
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `orphan linked appearance fails before destructive clear`() =
        runBlocking {
            val db = database()
            try {
                seedSentinel(db)
                var writerRan = false

                val failure =
                    runCatching {
                        restoreDataSource(db) {
                            writerRan = true
                            installTargets(db, it)
                        }.replaceWith(
                            canonicalBundle(
                                workspaces = listOf(workspace("workspace-1")),
                                hierarchyPlacements =
                                    listOf(
                                        snapshot(
                                            id = "real-p",
                                            targetId = "workspace-1",
                                        ),
                                    ),
                                linkedAppearances =
                                    listOf(
                                        linkedSnapshot(
                                            placementId = "missing-p",
                                        ),
                                    ),
                            ),
                        )
                    }.exceptionOrNull()

                assertNotNull(failure)
                assertFalse(writerRan)
                assertNotNull(db.workspaceDao().getById(SENTINEL_ID))
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    @Test
    fun `present empty H1 collection authoritatively clears dormant destination placements`() =
        runBlocking {
            val db = database()
            try {
                seedPlacement(db, targetId = "workspace-1")
                assertNotNull(db.hierarchyPlacementDao().getById("local-p"))

                restoreDataSource(db)
                    .replaceWith(
                        canonicalBundle(
                            workspaces = listOf(workspace("workspace-1")),
                            hierarchyPlacements = emptyList(),
                        ),
                    )

                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    @Test
    fun `old restore with absent H1 collection preserves dormant local H1 without materializing V1`() =
        runBlocking {
            val db = database()
            try {
                seedPlacement(db, targetId = "workspace-1", syncedAt = 55L)

                restoreDataSource(db)
                    .replaceWith(
                        canonicalBundle(
                            workspaces = listOf(workspace("workspace-1")),
                            hierarchyPlacements = null,
                        ),
                    )

                val preserved = requireNotNull(db.hierarchyPlacementDao().getById("local-p"))
                assertEquals("workspace-1", preserved.targetId)
                assertEquals(55L, preserved.syncedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `V absent H1 collection with no dormant graph does not materialize placements from canonical targets`() =
        runBlocking {
            val db = database()
            try {
                restoreDataSource(db)
                    .replaceWith(
                        canonicalBundle(
                            workspaces =
                                listOf(
                                    workspace("root"),
                                    workspace("child", parentWorkspaceId = "root"),
                                ),
                            hierarchyPlacements = null,
                        ),
                    )

                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    @Test
    fun `V2 restore translates legacy Workspace hierarchy once then uses native H1 restore path`() =
        runBlocking {
            val db = database()
            try {
                seedSentinel(db)

                val legacy =
                    canonicalBundle(
                        workspaces =
                            listOf(
                                workspace("root"),
                                workspace("child", parentWorkspaceId = "root"),
                            ),
                        hierarchyPlacements = null,
                    )

                // Production remains CURRENT during H4.0c.
                assertNull(
                    SnapshotRestoreCanonicalizerImpl()
                        .canonicalize(legacy)
                        .hierarchyPlacements,
                )

                val canonical =
                    SnapshotRestoreCanonicalizerImpl()
                        .canonicalize(
                            bundle = legacy,
                            hierarchyAuthorityMode =
                                HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                        )
                val translated = requireNotNull(canonical.hierarchyPlacements)
                assertEquals(2, translated.size)

                restoreDataSource(db).replaceWith(canonical)

                assertNull(db.workspaceDao().getById(SENTINEL_ID))

                val restored =
                    db.hierarchyPlacementDao()
                        .getAll()
                        .associateBy { it.id }

                assertEquals(translated.map { it.id }.toSet(), restored.keys)

                translated.forEach { expected ->
                    val actual = restored.getValue(expected.id)
                    assertEquals(expected.hierarchyId, actual.hierarchyId)
                    assertEquals(expected.targetType, actual.targetType)
                    assertEquals(expected.targetId, actual.targetId)
                    assertEquals(expected.parentPlacementId, actual.parentPlacementId)
                    assertEquals(expected.placementKind, actual.placementKind)
                    assertEquals(expected.siblingOrder, actual.siblingOrder)
                }

                val translatedRoot = translated.single { it.targetId == "root" }
                val translatedChild = translated.single { it.targetId == "child" }
                assertNull(translatedRoot.parentPlacementId)
                assertEquals(translatedRoot.id, translatedChild.parentPlacementId)

                val fkViolations =
                    db.openHelper.writableDatabase
                        .query("PRAGMA foreign_key_check")
                        .use { it.count }
                assertEquals(0, fkViolations)
            } finally {
                db.close()
            }
        }

    @Test
    fun `F missing parent fails before destructive clear`() = runBlocking {
        val db = database()
        try {
            seedSentinel(db)
            var writerRan = false
            val failure =
                runCatching {
                    restoreDataSource(db) {
                        writerRan = true
                        installTargets(db, it)
                    }.replaceWith(
                        canonicalBundle(
                            workspaces = listOf(workspace("workspace-1")),
                            hierarchyPlacements =
                                listOf(
                                    snapshot(
                                        id = "child",
                                        targetId = "workspace-1",
                                        parentId = "missing-parent",
                                    ),
                                ),
                        ),
                    )
                }.exceptionOrNull()

            assertNotNull(failure)
            assertFalse(writerRan)
            assertNotNull(db.workspaceDao().getById(SENTINEL_ID))
            assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `G cycle fails before destructive clear`() = runBlocking {
        val db = database()
        try {
            seedSentinel(db)
            var writerRan = false
            val failure =
                runCatching {
                    restoreDataSource(db) {
                        writerRan = true
                        installTargets(db, it)
                    }.replaceWith(
                        canonicalBundle(
                            workspaces =
                                listOf(
                                    workspace("workspace-a"),
                                    workspace("workspace-b"),
                                ),
                            hierarchyPlacements =
                                listOf(
                                    snapshot(
                                        id = "a",
                                        targetId = "workspace-a",
                                        parentId = "b",
                                    ),
                                    snapshot(
                                        id = "b",
                                        targetId = "workspace-b",
                                        parentId = "a",
                                    ),
                                ),
                        ),
                    )
                }.exceptionOrNull()

            assertNotNull(failure)
            assertFalse(writerRan)
            assertNotNull(db.workspaceDao().getById(SENTINEL_ID))
        } finally {
            db.close()
        }
    }

    @Test
    fun `H multiple PRIMARY placements fail before destructive clear`() = runBlocking {
        val db = database()
        try {
            seedSentinel(db)
            var writerRan = false
            val failure =
                runCatching {
                    restoreDataSource(db) {
                        writerRan = true
                        installTargets(db, it)
                    }.replaceWith(
                        canonicalBundle(
                            workspaces = listOf(workspace("workspace-1")),
                            hierarchyPlacements =
                                listOf(
                                    snapshot(id = "a", targetId = "workspace-1"),
                                    snapshot(id = "b", targetId = "workspace-1"),
                                ),
                        ),
                    )
                }.exceptionOrNull()

            assertNotNull(failure)
            assertFalse(writerRan)
            assertNotNull(db.workspaceDao().getById(SENTINEL_ID))
        } finally {
            db.close()
        }
    }

    @Test
    fun `I missing or tombstoned canonical target fails before destructive clear`() = runBlocking {
        val cases =
            listOf(
                canonicalBundle(
                    workspaces = emptyList(),
                    hierarchyPlacements =
                        listOf(snapshot(id = "missing", targetId = "not-present")),
                ),
                canonicalBundle(
                    workspaces = listOf(workspace("dead").copy(isDeleted = true)),
                    hierarchyPlacements =
                        listOf(snapshot(id = "dead-p", targetId = "dead")),
                ),
            )

        cases.forEachIndexed { index, bundle ->
            val db = database()
            try {
                seedSentinel(db)
                var writerRan = false

                val failure =
                    runCatching {
                        restoreDataSource(db) {
                            writerRan = true
                            installTargets(db, it)
                        }.replaceWith(bundle)
                    }.exceptionOrNull()

                assertNotNull("case $index must fail", failure)
                assertFalse("case $index writer must not run", writerRan)
                assertNotNull(db.workspaceDao().getById(SENTINEL_ID))
            } finally {
                db.close()
            }
        }
    }

    private fun restoreDataSource(
        db: AppDatabase,
        write: suspend (SnapshotBundle) -> Unit = { installTargets(db, it) },
    ) = SnapshotRestoreLocalDataSourceImpl(
        database = db,
        writer = CanonicalSnapshotTransactionWriter(write),
        dayManagementRuntimeRepository =
            mockk<DayManagementRuntimeRepository>(relaxed = true),
    )

    private suspend fun installTargets(
        db: AppDatabase,
        bundle: SnapshotBundle,
    ) {
        db.workspaceDao().upsert(requireNotNull(bundle.workspaces))
    }

    private suspend fun seedSentinel(db: AppDatabase) {
        db.workspaceDao().upsert(listOf(workspace(SENTINEL_ID)))
    }

    private suspend fun seedLinkedAppearance(
        db: AppDatabase,
        placementId: String,
        syncedAt: Long? = null,
    ) {
        db.hierarchyPlacementLinkedAppearanceDao().upsert(
            HierarchyPlacementLinkedAppearanceEntity(
                placementId = placementId,
                hierarchyId = "GENERAL",
                createdAt = 1L,
                updatedAt = 2L,
                syncedAt = syncedAt,
                isDeleted = false,
                version = 2L,
            ),
        )
    }

    private suspend fun seedPlacement(
        db: AppDatabase,
        targetId: String,
        syncedAt: Long? = null,
    ) {
        db.workspaceDao().upsert(listOf(workspace(targetId)))
        db.hierarchyPlacementDao().upsert(
            HierarchyPlacementEntity(
                id = "local-p",
                hierarchyId = "GENERAL",
                targetType = "WORKSPACE",
                targetId = targetId,
                parentPlacementId = null,
                placementKind = "PRIMARY",
                siblingOrder = 0L,
                createdAt = 1L,
                updatedAt = 2L,
                syncedAt = syncedAt,
                isDeleted = false,
                version = 2L,
            ),
        )
    }

    private fun canonicalBundle(
        workspaces: List<WorkspaceEntity>,
        hierarchyPlacements: List<HierarchyPlacementSnapshot>?,
        linkedAppearances: List<HierarchyPlacementLinkedAppearanceSnapshot>? = null,
    ) = SnapshotBundle(
        contexts = emptyList(),
        managedSubjects = emptyList(),
        orientations = emptyList(),
        aspects = emptyList(),
        orientationAssessments = emptyList(),
        orientationAssessmentRevisions = emptyList(),
        legacySubjectMappings = emptyList(),
        orientationRelations = emptyList(),
        aspectOrientationRefs = emptyList(),
        workspaces = workspaces,
        workspaceBindings = emptyList(),
        workspaceCapabilityInstances = emptyList(),
        savedOrientationViews = emptyList(),
        workspaceBacklogEntries = emptyList(),
        workspaceInboxRecords = emptyList(),
        hierarchyPlacements = hierarchyPlacements,
        hierarchyPlacementLinkedAppearances = linkedAppearances,
    )

    private fun workspace(
        id: String,
        parentWorkspaceId: String? = null,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = parentWorkspaceId,
        roleCode = null,
        workspaceOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId = null,
    )

    private fun linkedSnapshot(
        placementId: String,
        syncedAt: Long? = null,
        deleted: Boolean = false,
        version: Long = 1L,
    ) = HierarchyPlacementLinkedAppearanceSnapshot(
        placementId = placementId,
        hierarchyId = "GENERAL",
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = syncedAt,
        isDeleted = deleted,
        version = version,
    )

    private fun snapshot(
        id: String,
        targetId: String,
        parentId: String? = null,
        kind: String = "PRIMARY",
        order: Long = 0L,
        syncedAt: Long? = null,
        deleted: Boolean = false,
        version: Long = 1L,
    ) = HierarchyPlacementSnapshot(
        id = id,
        hierarchyId = "GENERAL",
        targetType = "WORKSPACE",
        targetId = targetId,
        parentPlacementId = parentId,
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = syncedAt,
        isDeleted = deleted,
        version = version,
    )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val SENTINEL_ID = "restore-h1-sentinel"
    }
}
