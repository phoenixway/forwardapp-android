package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalConnectionsRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `link unlink and compact order use canonical Connections placements`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedAttachment(database, "first")
            seedAttachment(database, "second")
            val repository = repository(database)

            val first = repository.linkAttachment("owner", "first", now = 10L)
            val second = repository.linkAttachment("owner", "second", now = 11L)
            assertEquals(listOf(first, second), database.workspaceConnectionDao().getLive("owner").map { it.id })

            repository.unlinkAttachment("owner", "first", now = 20L)

            val deleted = requireNotNull(database.workspaceConnectionDao().getById(first))
            val survivor = requireNotNull(database.workspaceConnectionDao().getById(second))
            assertTrue(deleted.isDeleted)
            assertEquals(0L, survivor.connectionOrder)
            assertEquals(2L, survivor.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `capability lifecycle preserves Connections placements and Attachments`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedAttachment(database, "attachment")
            val repository = repository(database)
            val id = repository.linkAttachment("owner", "attachment", now = 10L)

            repository.disable("owner", now = 20L)
            assertFalse(requireNotNull(database.workspaceConnectionDao().getById(id)).isDeleted)
            assertTrue(runCatching { repository.linkAttachment("owner", "other", now = 21L) }.isFailure)

            repository.archive("owner", now = 22L)
            repository.restore("owner", now = 23L)
            repository.enable("owner", now = 24L)
            repository.deleteCapability("owner", now = 25L)

            assertFalse(requireNotNull(database.workspaceConnectionDao().getById(id)).isDeleted)
            assertFalse(requireNotNull(database.attachmentDao().getAttachmentById("attachment")).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `reorder delegates order ownership to canonical Connections placements`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedAttachment(database, "first")
            seedAttachment(database, "second")
            seedAttachment(database, "third")
            val repository = repository(database)
            repository.linkAttachment("owner", "first", now = 10L)
            repository.linkAttachment("owner", "second", now = 11L)
            repository.linkAttachment("owner", "third", now = 12L)

            repository.reorder(
                workspaceId = "owner",
                orderedAttachmentIds = listOf("third", "first", "second"),
                now = 20L,
            )

            val reordered = database.workspaceConnectionDao().getLive("owner")
            assertEquals(listOf("third", "first", "second"), reordered.map { it.attachmentId })
            assertEquals(listOf(0L, 1L, 2L), reordered.map { it.connectionOrder })
            assertEquals(listOf(2L, 2L, 2L), reordered.map { it.version })
        } finally {
            database.close()
        }
    }

    @Test
    fun `reorder rejects incomplete placement set without mutation`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedAttachment(database, "first")
            seedAttachment(database, "second")
            val repository = repository(database)
            repository.linkAttachment("owner", "first", now = 10L)
            repository.linkAttachment("owner", "second", now = 11L)

            val failure =
                runCatching {
                    repository.reorder("owner", listOf("second"), now = 20L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(
                listOf("first", "second"),
                database.workspaceConnectionDao().getLive("owner").map { it.attachmentId },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `owner deletion tombstones placements without deleting Attachments`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedAttachment(database, "attachment")
            val repository = repository(database)
            val id = repository.linkAttachment("owner", "attachment", now = 10L)
            repository.disable("owner", now = 20L)

            assertEquals(1, repository.tombstoneOwnedContentForWorkspaces(listOf("owner"), now = 30L))
            assertTrue(requireNotNull(database.workspaceConnectionDao().getById(id)).isDeleted)
            assertFalse(requireNotNull(database.attachmentDao().getAttachmentById("attachment")).isDeleted)
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalConnectionsRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            connectionDao = database.workspaceConnectionDao(),
        )

    private suspend fun seedOwner(database: AppDatabase) {
        database.workspaceDao().upsert(listOf(workspace()))
        database.orientationDao().upsertWorkspaceCapabilities(listOf(capability()))
    }

    private suspend fun seedAttachment(database: AppDatabase, id: String) {
        database.attachmentDao().insertAttachment(
            AttachmentEntity(
                id = id,
                attachmentType = "LINK_ITEM",
                entityId = id,
                createdAt = 1L,
                updatedAt = 1L,
                version = 1L,
            ),
        )
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun workspace() =
        WorkspaceEntity(
            id = "owner",
            nameOverride = "Owner",
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

    private fun capability() =
        WorkspaceCapabilityInstanceEntity(
            id = "connections-owner",
            workspaceId = "owner",
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
}
