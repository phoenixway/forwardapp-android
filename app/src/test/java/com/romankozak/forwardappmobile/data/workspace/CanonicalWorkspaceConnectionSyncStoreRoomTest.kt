package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSyncVersion
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
class CanonicalWorkspaceConnectionSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical rows merge and expose unsynced and timestamp deltas`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)

            store.mergeIncoming(listOf(connection(updatedAt = 30L, version = 3L)))

            assertEquals(listOf("connection"), store.loadAll().map { it.id })
            assertEquals(listOf("connection"), store.loadUnsynced().map { it.id })
            assertEquals(listOf("connection"), store.loadChangedSince(20L).map { it.id })
            assertTrue(store.loadChangedSince(30L).isEmpty())

            val persisted = requireNotNull(database.workspaceConnectionDao().getById("connection"))
            assertEquals("owner", persisted.workspaceId)
            assertEquals("connections-owner", persisted.capabilityInstanceId)
            assertEquals("attachment", persisted.attachmentId)
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

            store.mergeIncoming(listOf(connection(version = 3L)))
            store.markSynced(listOf(WorkspaceConnectionSyncVersion("connection", 2L)))
            assertNull(database.workspaceConnectionDao().getById("connection")!!.syncedAt)

            store.markSynced(listOf(WorkspaceConnectionSyncVersion("connection", 3L)))
            assertNotNull(database.workspaceConnectionDao().getById("connection")!!.syncedAt)
            assertTrue(store.loadUnsynced().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `equal freshness tombstone wins and equal live cannot resurrect Connections`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)
            val live = connection(version = 5L, updatedAt = 50L, deleted = false)

            store.mergeIncoming(listOf(live))
            store.mergeIncoming(listOf(live.copy(isDeleted = true)))
            var persisted = requireNotNull(database.workspaceConnectionDao().getById("connection"))
            assertTrue(persisted.isDeleted)

            store.mergeIncoming(listOf(live))
            persisted = requireNotNull(database.workspaceConnectionDao().getById("connection"))
            assertTrue(persisted.isDeleted)
            assertEquals(5L, persisted.version)
            assertEquals(50L, persisted.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `owner capability attachment and createdAt identities are immutable`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            database.workspaceDao().upsert(listOf(workspace("owner-2")))
            database.orientationDao().upsertWorkspaceCapabilities(listOf(capability("owner-2")))
            database.attachmentDao().insertAttachment(attachment("attachment-2"))
            val store = store(database)
            val original = connection()

            store.mergeIncoming(listOf(original))

            val failure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            original.copy(
                                workspaceId = "owner-2",
                                capabilityInstanceId = "connections-owner-2",
                                attachmentId = "attachment-2",
                                createdAt = 2L,
                                version = 2L,
                                updatedAt = 20L,
                            ),
                        ),
                    )
                }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)

            val preserved = requireNotNull(database.workspaceConnectionDao().getById("connection"))
            assertEquals("owner", preserved.workspaceId)
            assertEquals("connections-owner", preserved.capabilityInstanceId)
            assertEquals("attachment", preserved.attachmentId)
            assertEquals(1L, preserved.createdAt)
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
        CanonicalWorkspaceConnectionSyncStore(
            database = database,
            connectionDao = database.workspaceConnectionDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            attachmentDao = database.attachmentDao(),
        )

    private suspend fun seedDependencies(database: AppDatabase) {
        database.workspaceDao().upsert(listOf(workspace("owner")))
        database.orientationDao().upsertWorkspaceCapabilities(listOf(capability("owner")))
        database.attachmentDao().insertAttachment(attachment("attachment"))
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
            id = "connections-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = "CONNECTIONS",
            instanceKey = "default",
            capabilityOrder = 1L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{}",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun attachment(id: String) =
        AttachmentEntity(
            id = id,
            attachmentType = "LINK_ITEM",
            entityId = id,
            createdAt = 1L,
            updatedAt = 1L,
            version = 1L,
        )

    private fun connection(
        workspaceId: String = "owner",
        capabilityInstanceId: String = "connections-owner",
        attachmentId: String = "attachment",
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) =
        WorkspaceConnectionSnapshot(
            id = "connection",
            workspaceId = workspaceId,
            capabilityInstanceId = capabilityInstanceId,
            attachmentId = attachmentId,
            order = 0L,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = deleted,
        )
}
