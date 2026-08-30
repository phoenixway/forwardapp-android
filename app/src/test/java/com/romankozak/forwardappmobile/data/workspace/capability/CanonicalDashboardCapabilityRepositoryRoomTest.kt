package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalDashboardCapabilityRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `kernel rejects retired capability activation`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "canonical"
            database.workspaceDao().upsert(listOf(canonicalWorkspace(workspaceId)))

            val failure =
                runCatching {
                    instanceStore(database).enable(
                        spec =
                            CanonicalCapabilityInstanceSpec(
                                type = WorkspaceCapabilityType.ARTIFACT,
                                configurationCodec = DashboardCapabilityConfigurationCodec,
                            ),
                        workspaceId = workspaceId,
                        now = 10L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(database.orientationDao().getAllWorkspaceCapabilities().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `kernel permits Context backed Workspace only with explicit cutover authority`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "legacy"
            database.workspaceDao().upsert(listOf(contextBackedWorkspace(workspaceId)))

            val id =
                instanceStore(database).enable(
                    spec =
                        CanonicalCapabilityInstanceSpec(
                            type = WorkspaceCapabilityType.DASHBOARD,
                            configurationCodec = DashboardCapabilityConfigurationCodec,
                            workspaceAuthority =
                                CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                        ),
                    workspaceId = workspaceId,
                    now = 10L,
                )

            assertEquals(id, dashboard(database, workspaceId).id)
        } finally {
            database.close()
        }
    }

    @Test
    fun `dashboard lifecycle reuses logical instance and restore is non activating`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "canonical"
            database.workspaceDao().upsert(listOf(canonicalWorkspace(workspaceId)))
            val repository = repository(database)

            val firstId = repository.enable(workspaceId, now = 10L)
            var instance = dashboard(database, workspaceId)

            assertEquals(firstId, instance.id)
            assertEquals(WorkspaceCapabilityState.ACTIVE.name, instance.state)
            assertEquals("{}", instance.configuration)
            assertEquals(1, instance.configurationVersion)
            assertEquals(1L, instance.version)

            repository.disable(workspaceId, now = 20L)
            instance = dashboard(database, workspaceId)
            assertEquals(WorkspaceCapabilityState.DISABLED.name, instance.state)
            assertEquals(2L, instance.version)

            repository.archive(workspaceId, now = 30L)
            instance = dashboard(database, workspaceId)
            assertEquals(WorkspaceCapabilityState.ARCHIVED.name, instance.state)
            assertEquals(3L, instance.version)

            val enableArchivedFailure =
                runCatching { repository.enable(workspaceId, now = 35L) }.exceptionOrNull()
            assertTrue(enableArchivedFailure is IllegalArgumentException)

            val disableArchivedFailure =
                runCatching { repository.disable(workspaceId, now = 36L) }.exceptionOrNull()
            assertTrue(disableArchivedFailure is IllegalArgumentException)

            instance = dashboard(database, workspaceId)
            assertEquals(WorkspaceCapabilityState.ARCHIVED.name, instance.state)
            assertEquals(3L, instance.version)

            repository.restore(workspaceId, now = 40L)
            instance = dashboard(database, workspaceId)
            assertEquals(WorkspaceCapabilityState.DISABLED.name, instance.state)
            assertEquals(4L, instance.version)

            val secondId = repository.enable(workspaceId, now = 50L)
            assertEquals(firstId, secondId)

            instance = dashboard(database, workspaceId)
            assertEquals(WorkspaceCapabilityState.ACTIVE.name, instance.state)
            assertEquals(5L, instance.version)

            assertEquals(
                1,
                database.orientationDao().getAllWorkspaceCapabilities()
                    .count {
                        it.workspaceId == workspaceId &&
                            it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name &&
                            it.instanceKey == "default"
                    },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `delete tombstones metadata and explicit enable resurrects same logical instance`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "canonical"
            database.workspaceDao().upsert(listOf(canonicalWorkspace(workspaceId)))
            val repository = repository(database)

            val id = repository.enable(workspaceId, now = 10L)
            repository.delete(workspaceId, now = 20L)

            var instance = dashboard(database, workspaceId)
            assertTrue(instance.isDeleted)
            assertEquals(2L, instance.version)

            val restoredId = repository.enable(workspaceId, now = 30L)

            assertEquals(id, restoredId)
            instance = dashboard(database, workspaceId)
            assertFalse(instance.isDeleted)
            assertEquals(WorkspaceCapabilityState.ACTIVE.name, instance.state)
            assertEquals(3L, instance.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `context backed Workspace rejects Dashboard canonical commands`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(contextBackedWorkspace("legacy")))
            val repository = repository(database)

            val failure =
                runCatching {
                    repository.enable("legacy", now = 10L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(database.orientationDao().getAllWorkspaceCapabilities().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `unknown Dashboard configuration version blocks mutation and preserves raw row`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "canonical"
            database.workspaceDao().upsert(listOf(canonicalWorkspace(workspaceId)))

            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    WorkspaceCapabilityInstanceEntity(
                        id = "future-dashboard",
                        workspaceId = workspaceId,
                        capabilityType = WorkspaceCapabilityType.DASHBOARD.name,
                        instanceKey = "default",
                        capabilityOrder = 0L,
                        state = WorkspaceCapabilityState.DISABLED.name,
                        configurationVersion = 2,
                        configuration = """{"future":true}""",
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 7L,
                    ),
                ),
            )

            val repository = repository(database)
            val failure =
                runCatching {
                    repository.enable(workspaceId, now = 10L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)

            val preserved = dashboard(database, workspaceId)
            assertEquals(2, preserved.configurationVersion)
            assertEquals("""{"future":true}""", preserved.configuration)
            assertEquals(WorkspaceCapabilityState.DISABLED.name, preserved.state)
            assertEquals(7L, preserved.version)
            assertFalse(preserved.isDeleted)
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalDashboardCapabilityRepository(
            instanceStore = instanceStore(database),
        )

    private fun instanceStore(database: AppDatabase) =
        CanonicalCapabilityInstanceStore(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
        )

    private suspend fun dashboard(
        database: AppDatabase,
        workspaceId: String,
    ) =
        database.orientationDao().getAllWorkspaceCapabilities().single {
            it.workspaceId == workspaceId &&
                it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name &&
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
