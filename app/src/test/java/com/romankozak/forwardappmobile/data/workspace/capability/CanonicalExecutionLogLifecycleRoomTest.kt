package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalExecutionLogLifecycleRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `lifecycle preserves logical identity and restore is non activating`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(canonicalWorkspace("canonical")))
            val repository = repository(database)

            val id = repository.enable("canonical", now = 10L)

            repository.disable("canonical", now = 20L)
            assertEquals(
                WorkspaceCapabilityState.DISABLED.name,
                capability(database, "canonical").state,
            )

            repository.archive("canonical", now = 30L)
            assertEquals(
                WorkspaceCapabilityState.ARCHIVED.name,
                capability(database, "canonical").state,
            )

            repository.restore("canonical", now = 40L)
            assertEquals(
                WorkspaceCapabilityState.DISABLED.name,
                capability(database, "canonical").state,
            )

            repository.enable("canonical", now = 50L)
            repository.delete("canonical", now = 60L)

            val deleted = capability(database, "canonical")
            assertTrue(deleted.isDeleted)
            assertEquals(id, deleted.id)

            val resurrectedId = repository.enable("canonical", now = 70L)
            val resurrected = capability(database, "canonical")

            assertEquals(id, resurrectedId)
            assertEquals(id, resurrected.id)
            assertEquals(WorkspaceCapabilityState.ACTIVE.name, resurrected.state)
            assertTrue(!resurrected.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical lifecycle rejects Context backed workspace and unknown configuration`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(
                listOf(
                    canonicalWorkspace("canonical"),
                    contextBackedWorkspace("legacy"),
                ),
            )
            val repository = repository(database)

            val legacyFailure =
                runCatching { repository.enable("legacy", now = 10L) }.exceptionOrNull()
            assertTrue(legacyFailure is IllegalArgumentException)

            repository.enable("canonical", now = 20L)
            val current = capability(database, "canonical")

            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    current.copy(
                        configurationVersion = 2,
                        configuration = """{"future":true}""",
                    ),
                ),
            )

            val configFailure =
                runCatching { repository.disable("canonical", now = 30L) }.exceptionOrNull()

            assertTrue(configFailure is IllegalArgumentException)
            assertEquals(
                2,
                capability(database, "canonical").configurationVersion,
            )
            assertEquals(
                """{"future":true}""",
                capability(database, "canonical").configuration,
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

    private suspend fun capability(
        database: AppDatabase,
        workspaceId: String,
    ): WorkspaceCapabilityInstanceEntity =
        database.orientationDao().getAllWorkspaceCapabilities().single {
            it.workspaceId == workspaceId &&
                it.capabilityType == WorkspaceCapabilityType.EXECUTION_LOG.name &&
                it.instanceKey == "default"
        }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

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

    private fun contextBackedWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            nameOverride = "Legacy",
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )
}
