package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalInboxRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create update reorder and tombstone use one canonical collection`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            val repository = repository(database)
            val first = repository.createRecord("owner", "First", now = 10L)
            val second = repository.createRecord("owner", "Second", now = 11L)

            assertEquals(listOf(first, second), repository.getRecords("owner").map { it.id })
            repository.updateRecord(first, "First updated", now = 20L)
            repository.reorder("owner", listOf(second, first), now = 21L)

            var updated = requireNotNull(repository.getRecord(first))
            assertEquals("First updated", updated.text)
            assertEquals(1L, updated.recordOrder)
            assertEquals(3L, updated.version)
            assertNull(updated.syncedAt)

            repository.tombstoneRecord(second, now = 30L)
            val deleted = requireNotNull(repository.getRecord(second))
            updated = requireNotNull(repository.getRecord(first))
            assertTrue(deleted.isDeleted)
            assertEquals(0L, updated.recordOrder)
            assertEquals(listOf(first), repository.getRecords("owner").map { it.id })
        } finally {
            database.close()
        }
    }

    @Test
    fun `capability lifecycle preserves Inbox content`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            val repository = repository(database)
            val id = repository.createRecord("owner", "Preserve", now = 10L)

            repository.disable("owner", now = 20L)
            assertFalse(requireNotNull(repository.getRecord(id)).isDeleted)
            assertTrue(runCatching { repository.createRecord("owner", "Blocked", now = 21L) }.isFailure)

            repository.archive("owner", now = 22L)
            repository.restore("owner", now = 23L)
            repository.enable("owner", now = 24L)
            repository.deleteCapability("owner", now = 25L)

            assertFalse(requireNotNull(repository.getRecord(id)).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `owner deletion tombstones content without active capability guard`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            val repository = repository(database)
            val first = repository.createRecord("owner", "First", now = 10L)
            val second = repository.createRecord("owner", "Second", now = 11L)
            repository.disable("owner", now = 20L)

            assertEquals(
                2,
                repository.tombstoneOwnedContentForWorkspaces(listOf("owner"), now = 30L),
            )
            assertTrue(requireNotNull(repository.getRecord(first)).isDeleted)
            assertTrue(requireNotNull(repository.getRecord(second)).isDeleted)
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalInboxRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            recordDao = database.workspaceInboxRecordDao(),
        )

    private suspend fun seedOwner(database: AppDatabase) {
        database.workspaceDao().upsert(listOf(workspace()))
        database.orientationDao().upsertWorkspaceCapabilities(listOf(capability()))
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
            id = "inbox-owner",
            workspaceId = "owner",
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
}
