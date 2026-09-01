package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
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
class CanonicalWorkspaceInboxSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical rows merge and expose unsynced and timestamp deltas`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)

            store.mergeIncoming(listOf(record(updatedAt = 30L, version = 3L)))

            assertEquals(listOf("inbox"), store.loadAll().map { it.id })
            assertEquals(listOf("inbox"), store.loadUnsynced().map { it.id })
            assertEquals(listOf("inbox"), store.loadChangedSince(20L).map { it.id })
            assertTrue(store.loadChangedSince(30L).isEmpty())

            val persisted = requireNotNull(database.workspaceInboxRecordDao().getById("inbox"))
            assertEquals("owner", persisted.workspaceId)
            assertEquals("inbox-owner", persisted.capabilityInstanceId)
            assertEquals(3L, persisted.version)
            assertNull(persisted.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `exact version acknowledgement marks only matching canonical rows synced`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)

            store.mergeIncoming(listOf(record(version = 3L)))
            store.markSynced(listOf(WorkspaceInboxRecordSyncVersion("inbox", 2L)))
            assertNull(database.workspaceInboxRecordDao().getById("inbox")!!.syncedAt)

            store.markSynced(listOf(WorkspaceInboxRecordSyncVersion("inbox", 3L)))
            assertNotNull(database.workspaceInboxRecordDao().getById("inbox")!!.syncedAt)
            assertTrue(store.loadUnsynced().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `equal freshness tombstone wins and equal live cannot resurrect Inbox`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)
            val live = record(version = 5L, updatedAt = 50L, deleted = false)

            store.mergeIncoming(listOf(live))
            store.mergeIncoming(listOf(live.copy(isDeleted = true)))
            var persisted = requireNotNull(database.workspaceInboxRecordDao().getById("inbox"))
            assertTrue(persisted.isDeleted)

            store.mergeIncoming(listOf(live))
            persisted = requireNotNull(database.workspaceInboxRecordDao().getById("inbox"))
            assertTrue(persisted.isDeleted)
            assertEquals(5L, persisted.version)
            assertEquals(50L, persisted.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `owner and capability identities are immutable`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            database.workspaceDao().upsert(listOf(workspace("owner-2")))
            database.orientationDao().upsertWorkspaceCapabilities(listOf(capability("owner-2")))
            val store = store(database)
            val original = record()

            store.mergeIncoming(listOf(original))

            val ownerFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            original.copy(
                                workspaceId = "owner-2",
                                capabilityInstanceId = "inbox-owner-2",
                                version = 2L,
                                updatedAt = 20L,
                            ),
                        ),
                    )
                }.exceptionOrNull()
            assertTrue(ownerFailure is IllegalArgumentException)

            val preserved = requireNotNull(database.workspaceInboxRecordDao().getById("inbox"))
            assertEquals("owner", preserved.workspaceId)
            assertEquals("inbox-owner", preserved.capabilityInstanceId)
            assertEquals(1L, preserved.version)
            assertFalse(preserved.isDeleted)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun store(database: AppDatabase) =
        CanonicalWorkspaceInboxSyncStore(
            database = database,
            recordDao = database.workspaceInboxRecordDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
        )

    private suspend fun seedDependencies(database: AppDatabase) {
        database.workspaceDao().upsert(listOf(workspace("owner")))
        database.orientationDao().upsertWorkspaceCapabilities(listOf(capability("owner")))
    }

    private fun workspace(id: String) =
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
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
        )

    private fun capability(workspaceId: String) =
        WorkspaceCapabilityInstanceEntity(
            id = "inbox-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = "INBOX",
            instanceKey = "default",
            capabilityOrder = 1L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{\"ownerVisibility\":\"KEEP_VISIBLE\"}",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun record(
        workspaceId: String = "owner",
        capabilityInstanceId: String = "inbox-owner",
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) =
        WorkspaceInboxRecordSnapshot(
            id = "inbox",
            workspaceId = workspaceId,
            capabilityInstanceId = capabilityInstanceId,
            text = "Inbox record",
            order = 0L,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = deleted,
        )
}
