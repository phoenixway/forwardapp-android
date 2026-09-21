package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
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
class CanonicalHierarchyPlacementSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `B null peer payload leaves local placements untouched`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(entity(id = "p1", targetId = "workspace-1"))
            val store = CanonicalHierarchyPlacementSyncStore(db)

            store.mergeIncoming(null)

            assertEquals("p1", requireNotNull(db.hierarchyPlacementDao().getById("p1")).id)
        } finally {
            db.close()
        }
    }

    @Test
    fun `D persisted transport mapping preserves all placement fields`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    parentId = null,
                    kind = "LINK",
                    order = 42L,
                    createdAt = 100L,
                    updatedAt = 200L,
                    syncedAt = 175L,
                    deleted = true,
                    version = 7L,
                ),
            )

            val row = CanonicalHierarchyPlacementSyncStore(db).loadAll().single()

            assertEquals("p1", row.id)
            assertEquals("GENERAL", row.hierarchyId)
            assertEquals("WORKSPACE", row.targetType)
            assertEquals("workspace-1", row.targetId)
            assertNull(row.parentPlacementId)
            assertEquals("LINK", row.placementKind)
            assertEquals(42L, row.siblingOrder)
            assertEquals(100L, row.createdAt)
            assertEquals(200L, row.updatedAt)
            assertEquals(175L, row.syncedAt)
            assertTrue(row.isDeleted)
            assertEquals(7L, row.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun `J higher version peer row wins and S accepted remote syncedAt becomes local unsynced`() =
        runBlocking {
            val db = database()
            try {
                workspace(db, "workspace-1")
                db.hierarchyPlacementDao().upsert(
                    entity(
                        id = "p1",
                        targetId = "workspace-1",
                        order = 0L,
                        updatedAt = 10L,
                        syncedAt = 9L,
                        version = 1L,
                    ),
                )
                val store = CanonicalHierarchyPlacementSyncStore(db)

                store.mergeIncoming(
                    listOf(
                        snapshot(
                            id = "p1",
                            targetId = "workspace-1",
                            order = 5L,
                            updatedAt = 20L,
                            syncedAt = 999L,
                            version = 2L,
                        ),
                    ),
                )

                val stored = requireNotNull(db.hierarchyPlacementDao().getById("p1"))
                assertEquals(5L, stored.siblingOrder)
                assertEquals(20L, stored.updatedAt)
                assertEquals(2L, stored.version)
                assertNull(stored.syncedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `K lower version live row cannot resurrect newer tombstone`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    updatedAt = 30L,
                    deleted = true,
                    version = 3L,
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            store.mergeIncoming(
                listOf(
                    snapshot(
                        id = "p1",
                        targetId = "workspace-1",
                        updatedAt = 20L,
                        deleted = false,
                        version = 2L,
                    ),
                ),
            )

            val stored = requireNotNull(db.hierarchyPlacementDao().getById("p1"))
            assertTrue(stored.isDeleted)
            assertEquals(3L, stored.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun `L exact freshness semantic equality is idempotent and keeps local ACK metadata`() =
        runBlocking {
            val db = database()
            try {
                workspace(db, "workspace-1")
                db.hierarchyPlacementDao().upsert(
                    entity(
                        id = "p1",
                        targetId = "workspace-1",
                        updatedAt = 20L,
                        syncedAt = 123L,
                        version = 2L,
                    ),
                )
                val store = CanonicalHierarchyPlacementSyncStore(db)

                store.mergeIncoming(
                    listOf(
                        snapshot(
                            id = "p1",
                            targetId = "workspace-1",
                            updatedAt = 20L,
                            syncedAt = 999L,
                            version = 2L,
                        ),
                    ),
                )

                val stored = requireNotNull(db.hierarchyPlacementDao().getById("p1"))
                assertEquals(123L, stored.syncedAt)
                assertEquals(2L, stored.version)
            } finally {
                db.close()
            }
        }

    @Test
    fun `M equal freshness semantic divergence fails closed`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    order = 0L,
                    updatedAt = 20L,
                    version = 2L,
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            val failure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            snapshot(
                                id = "p1",
                                targetId = "workspace-1",
                                order = 9L,
                                updatedAt = 20L,
                                version = 2L,
                            ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is HierarchyPlacementMergeConflictException)
            assertEquals(0L, requireNotNull(db.hierarchyPlacementDao().getById("p1")).siblingOrder)
        } finally {
            db.close()
        }
    }

    @Test
    fun `I missing or tombstoned target rejects peer merge atomically`() = runBlocking {
        val db = database()
        try {
            workspace(db, "dead")
            tombstoneWorkspace(db, "dead")
            val store = CanonicalHierarchyPlacementSyncStore(db)

            val missingFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(snapshot(id = "missing-p", targetId = "missing")),
                    )
                }.exceptionOrNull()
            assertTrue(missingFailure is HierarchyTargetMissingException)
            assertNull(db.hierarchyPlacementDao().getById("missing-p"))

            val deletedFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(snapshot(id = "dead-p", targetId = "dead")),
                    )
                }.exceptionOrNull()
            assertTrue(deletedFailure is HierarchyTargetDeletedException)
            assertNull(db.hierarchyPlacementDao().getById("dead-p"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `N graph-wide PRIMARY conflict rejects all incoming rows atomically`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            workspace(db, "workspace-2")
            db.hierarchyPlacementDao().upsert(
                entity(id = "existing", targetId = "workspace-1"),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            val failure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            snapshot(id = "conflict", targetId = "workspace-1"),
                            snapshot(id = "otherwise-valid", targetId = "workspace-2"),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is PrimaryAppearanceConflictException)
            assertNull(db.hierarchyPlacementDao().getById("conflict"))
            assertNull(db.hierarchyPlacementDao().getById("otherwise-valid"))
            assertNotNull(db.hierarchyPlacementDao().getById("existing"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `O cycle introduced by winner rejects merge and preserves old graph`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-a")
            workspace(db, "workspace-b")
            db.hierarchyPlacementDao().upsertAll(
                listOf(
                    entity(
                        id = "a",
                        targetId = "workspace-a",
                        updatedAt = 10L,
                        version = 1L,
                    ),
                    entity(
                        id = "b",
                        targetId = "workspace-b",
                        parentId = "a",
                        updatedAt = 10L,
                        version = 1L,
                    ),
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            val failure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            snapshot(
                                id = "a",
                                targetId = "workspace-a",
                                parentId = "b",
                                updatedAt = 20L,
                                version = 2L,
                            ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is HierarchyPlacementCycleException)
            assertNull(requireNotNull(db.hierarchyPlacementDao().getById("a")).parentPlacementId)
            assertEquals("a", requireNotNull(db.hierarchyPlacementDao().getById("b")).parentPlacementId)
        } finally {
            db.close()
        }
    }

    @Test
    fun `equal freshness live versus tombstone resolves to tombstone`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    updatedAt = 20L,
                    syncedAt = 18L,
                    deleted = false,
                    version = 2L,
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            store.mergeIncoming(
                listOf(
                    snapshot(
                        id = "p1",
                        targetId = "workspace-1",
                        updatedAt = 20L,
                        syncedAt = 999L,
                        deleted = true,
                        version = 2L,
                    ),
                ),
            )

            val stored = requireNotNull(db.hierarchyPlacementDao().getById("p1"))
            assertTrue(stored.isDeleted)
            assertNull(stored.syncedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `P exact version ACK marks current unsynced row`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    syncedAt = null,
                    version = 3L,
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            store.markSynced(listOf(HierarchyPlacementSyncVersion("p1", 3L)))

            assertNotNull(requireNotNull(db.hierarchyPlacementDao().getById("p1")).syncedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Q stale ACK cannot mark a newer local version`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    syncedAt = null,
                    version = 4L,
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            store.markSynced(listOf(HierarchyPlacementSyncVersion("p1", 3L)))

            assertNull(requireNotNull(db.hierarchyPlacementDao().getById("p1")).syncedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `R tombstone is outbound while unsynced and ACKable`() = runBlocking {
        val db = database()
        try {
            workspace(db, "workspace-1")
            db.hierarchyPlacementDao().upsert(
                entity(
                    id = "p1",
                    targetId = "workspace-1",
                    deleted = true,
                    syncedAt = null,
                    version = 5L,
                ),
            )
            val store = CanonicalHierarchyPlacementSyncStore(db)

            val outbound = store.loadUnsynced()
            assertEquals(listOf("p1"), outbound.map { it.id })
            assertTrue(outbound.single().isDeleted)

            store.markSynced(listOf(HierarchyPlacementSyncVersion("p1", 5L)))
            assertNotNull(requireNotNull(db.hierarchyPlacementDao().getById("p1")).syncedAt)
            assertTrue(store.loadUnsynced().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `T malformed hierarchy target type or placement kind fails strict decode`() = runBlocking {
        val db = database()
        try {
            val store = CanonicalHierarchyPlacementSyncStore(db)
            val badRows =
                listOf(
                    snapshot(id = "bad-h", targetId = "x").copy(hierarchyId = "OTHER"),
                    snapshot(id = "bad-t", targetId = "x").copy(targetType = "CONTEXT"),
                    snapshot(id = "bad-k", targetId = "x").copy(placementKind = "MAGIC"),
                )

            badRows.forEach { row ->
                val failure =
                    runCatching { store.mergeIncoming(listOf(row)) }.exceptionOrNull()
                assertTrue(
                    "Expected strict decode failure for ${row.id}, got $failure",
                    failure is IllegalArgumentException,
                )
            }
            assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
        } finally {
            db.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun workspace(
        db: AppDatabase,
        id: String,
    ) {
        db.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private suspend fun tombstoneWorkspace(
        db: AppDatabase,
        id: String,
    ) {
        val current = requireNotNull(db.workspaceDao().getById(id))
        db.workspaceDao().upsert(
            listOf(
                current.copy(
                    updatedAt = current.updatedAt + 1L,
                    syncedAt = null,
                    isDeleted = true,
                    version = current.version + 1L,
                ),
            ),
        )
    }

    private fun entity(
        id: String,
        targetId: String,
        parentId: String? = null,
        kind: String = "PRIMARY",
        order: Long = 0L,
        createdAt: Long = 1L,
        updatedAt: Long = createdAt,
        syncedAt: Long? = null,
        deleted: Boolean = false,
        version: Long = 1L,
    ) = HierarchyPlacementEntity(
        id = id,
        hierarchyId = "GENERAL",
        targetType = "WORKSPACE",
        targetId = targetId,
        parentPlacementId = parentId,
        placementKind = kind,
        siblingOrder = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
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
        createdAt: Long = 1L,
        updatedAt: Long = createdAt,
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
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = deleted,
        version = version,
    )
}
