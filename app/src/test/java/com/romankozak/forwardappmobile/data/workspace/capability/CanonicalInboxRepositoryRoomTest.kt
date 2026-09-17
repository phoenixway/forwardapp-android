package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    fun `configuration update persists typed Inbox configuration`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            val repository = repository(database)
            val configuration =
                InboxCapabilityConfigurationV1(
                    ownerVisibility = InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED,
                )

            repository.updateConfiguration(
                workspaceId = "owner",
                configuration = configuration,
                now = 20L,
            )

            val stored =
                database.orientationDao()
                    .getAllWorkspaceCapabilities()
                    .single {
                        it.workspaceId == "owner" &&
                            it.capabilityType == "INBOX" &&
                            !it.isDeleted
                    }
            assertEquals(
                configuration,
                InboxCapabilityConfigurationCodec.decode(
                    stored.configurationVersion,
                    stored.configuration,
                ),
            )
            assertEquals(2L, stored.version)
            assertEquals(20L, stored.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `typed state preserves lifecycle deletion and configuration without version churn`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(workspace()))
            val repository = repository(database)
            assertNull(repository.getState("owner"))
            val active = CompletableDeferred<Unit>()
            val disabled = CompletableDeferred<Unit>()
            val archived = CompletableDeferred<Unit>()
            val deleted = CompletableDeferred<Unit>()
            val observation = launch {
                repository.observeState("owner").collect { state ->
                    when {
                        state?.isDeleted == true -> deleted.complete(Unit)
                        state?.lifecycleState == WorkspaceCapabilityState.ARCHIVED -> archived.complete(Unit)
                        state?.lifecycleState == WorkspaceCapabilityState.DISABLED -> disabled.complete(Unit)
                        state?.lifecycleState == WorkspaceCapabilityState.ACTIVE -> active.complete(Unit)
                    }
                }
            }
            try {
                repository.enable("owner", now = 10L)
                withTimeout(5_000L) { active.await() }
                assertEquals(WorkspaceCapabilityState.ACTIVE, repository.getState("owner")?.lifecycleState)
                val configuration = InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED)
                repository.updateConfiguration("owner", configuration, now = 20L)
                val version = database.orientationDao().getAllWorkspaceCapabilities().single().version
                repository.updateConfiguration("owner", configuration, now = 30L)
                assertEquals(version, database.orientationDao().getAllWorkspaceCapabilities().single().version)
                assertEquals(configuration, repository.observeState("owner").first()?.configuration)

                repository.disable("owner", now = 40L)
                withTimeout(5_000L) { disabled.await() }
                assertEquals(WorkspaceCapabilityState.DISABLED, repository.getState("owner")?.lifecycleState)
                repository.archive("owner", now = 50L)
                withTimeout(5_000L) { archived.await() }
                assertEquals(WorkspaceCapabilityState.ARCHIVED, repository.getState("owner")?.lifecycleState)
                repository.deleteCapability("owner", now = 60L)
                withTimeout(5_000L) { deleted.await() }
                assertTrue(requireNotNull(repository.getState("owner")).isDeleted)
            } finally {
                observation.cancelAndJoin()
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `typed state rejects malformed configuration`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(workspace()))
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(capability().copy(configurationVersion = 999)),
            )

            assertTrue(runCatching { repository(database).getState("owner") }.isFailure)
        } finally {
            database.close()
        }
    }

    @Test
    fun `database uniqueness prevents duplicate Inbox logical default`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            val before = database.orientationDao().getAllWorkspaceCapabilities().single()

            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(before.copy(id = "duplicate-inbox")),
            )

            val after = database.orientationDao().getAllWorkspaceCapabilities()
            assertEquals(1, after.size)
            assertEquals(before, after.single())
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
