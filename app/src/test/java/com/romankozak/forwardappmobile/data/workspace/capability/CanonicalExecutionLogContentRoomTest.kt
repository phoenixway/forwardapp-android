package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalExecutionLogContentRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create update delete use canonical row shape and sync version contract`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(canonicalWorkspace("canonical")))
            val repository = repository(database)
            repository.enable("canonical", now = 10L)

            val id =
                repository.createLog(
                    workspaceId = "canonical",
                    type = "COMMENT",
                    description = "Initial",
                    details = "A",
                    timestamp = 20L,
                    now = 21L,
                )

            var log = database.contextManagementDao().getLogById(id)!!
            assertNull(log.contextId)
            assertEquals("canonical", log.workspaceId)
            assertEquals(20L, log.timestamp)
            assertEquals(21L, log.updatedAt)
            assertEquals(1L, log.version)
            assertNull(log.syncedAt)
            assertTrue(!log.isDeleted)

            repository.updateLog(
                workspaceId = "canonical",
                logId = id,
                type = "INSIGHT",
                description = "Updated",
                details = null,
                now = 30L,
            )

            log = database.contextManagementDao().getLogById(id)!!
            assertEquals("INSIGHT", log.type)
            assertEquals("Updated", log.description)
            assertEquals(20L, log.timestamp)
            assertEquals(30L, log.updatedAt)
            assertEquals(2L, log.version)
            assertNull(log.syncedAt)

            repository.deleteLog(
                workspaceId = "canonical",
                logId = id,
                now = 40L,
            )

            log = database.contextManagementDao().getLogById(id)!!
            assertTrue(log.isDeleted)
            assertEquals(40L, log.updatedAt)
            assertEquals(3L, log.version)
            assertNull(log.syncedAt)
            assertTrue(repository.getLiveLogs("canonical").isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context backed Workspace uses canonical rows and system audit survives disabled capability`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(contextBackedWorkspace("context-backed")))
            val repository = repository(database)
            repository.enable("context-backed", now = 10L)

            val authoredId =
                repository.createLog(
                    workspaceId = "context-backed",
                    type = "COMMENT",
                    description = "User",
                    timestamp = 20L,
                    now = 20L,
                )
            val authored = database.contextManagementDao().getLogById(authoredId)!!
            assertNull(authored.contextId)
            assertEquals("context-backed", authored.workspaceId)

            repository.disable("context-backed", now = 30L)

            val userFailure =
                runCatching {
                    repository.createLog(
                        workspaceId = "context-backed",
                        type = "COMMENT",
                        description = "Blocked user write",
                        timestamp = 31L,
                        now = 31L,
                    )
                }.exceptionOrNull()
            assertTrue(userFailure is IllegalArgumentException)

            val systemId =
                repository.createSystemLog(
                    workspaceId = "context-backed",
                    type = "AUTOMATIC",
                    description = "System audit",
                    timestamp = 32L,
                    now = 32L,
                )
            val system = database.contextManagementDao().getLogById(systemId)!!
            assertNull(system.contextId)
            assertEquals("context-backed", system.workspaceId)
            assertEquals("System audit", system.description)
        } finally {
            database.close()
        }
    }

    @Test
    fun `owner deletion cascade tombstones live logs without active capability guard`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(contextBackedWorkspace("context-backed")))
            val repository = repository(database)
            repository.enable("context-backed", now = 10L)
            val logId =
                repository.createLog(
                    workspaceId = "context-backed",
                    type = "COMMENT",
                    description = "Owned log",
                    timestamp = 20L,
                    now = 20L,
                )
            repository.disable("context-backed", now = 30L)

            val changed =
                repository.tombstoneOwnedContentForWorkspaces(
                    workspaceIds = listOf("context-backed"),
                    now = 40L,
                )

            assertEquals(1, changed)
            val tombstone = requireNotNull(database.contextManagementDao().getLogById(logId))
            assertTrue(tombstone.isDeleted)
            assertEquals(2L, tombstone.version)
            assertEquals(40L, tombstone.updatedAt)
            assertNull(tombstone.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `authoring requires active capability and cannot mutate legacy row`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(canonicalWorkspace("canonical")))
            val repository = repository(database)
            repository.enable("canonical", now = 10L)
            repository.disable("canonical", now = 20L)

            val disabledFailure =
                runCatching {
                    repository.createLog(
                        workspaceId = "canonical",
                        type = "COMMENT",
                        description = "Blocked",
                        timestamp = 21L,
                        now = 21L,
                    )
                }.exceptionOrNull()
            assertTrue(disabledFailure is IllegalArgumentException)

            repository.enable("canonical", now = 30L)

            database.contextDao().insert(
                ContextEntity(
                    id = "legacy-context",
                    name = "Legacy",
                    description = null,
                    parentId = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    roleCode = "management",
                ),
            )
            database.contextManagementDao().insertLog(
                ContextLog(
                    id = "legacy-log",
                    contextId = "legacy-context",
                    timestamp = 2L,
                    type = "COMMENT",
                    description = "Legacy",
                    updatedAt = 2L,
                    version = 1L,
                ),
            )

            val legacyFailure =
                runCatching {
                    repository.updateLog(
                        workspaceId = "canonical",
                        logId = "legacy-log",
                        type = "COMMENT",
                        description = "Must fail",
                        now = 31L,
                    )
                }.exceptionOrNull()

            assertTrue(legacyFailure is IllegalArgumentException)
            assertEquals(
                "Legacy",
                database.contextManagementDao().getLogById("legacy-log")?.description,
            )
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalExecutionLogRepository(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextManagementDao = database.contextManagementDao(),
            instanceStore = instanceStore(database),
        )

    private fun instanceStore(database: AppDatabase) =
        CanonicalCapabilityInstanceStore(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun contextBackedWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            nameOverride = "Context-backed",
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    private fun canonicalWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Canonical",
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
        )
}
