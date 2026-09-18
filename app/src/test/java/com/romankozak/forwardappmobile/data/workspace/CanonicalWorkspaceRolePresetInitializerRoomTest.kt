package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceRolePresetInitializerRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test
    fun `management preset initializes canonical capabilities without Context shell`() = runBlocking {
        val database = database()
        try {
            val repositories = repositories(database)
            val workspaceId = "operations-workspace"
            seedStandaloneWorkspace(
                database = database,
                id = workspaceId,
                name = "Operations",
                roleCode = "management",
                now = 10L,
            )

            database.structurePresetDao().insertPreset(
                ContextRoleProfile(
                    id = "management-preset",
                    code = "management",
                    label = "Management",
                    description = null,
                    enableInbox = true,
                    enableLog = false,
                    enableDashboard = false,
                    enableBacklog = true,
                    enableAttachments = false,
                    enableAutoLinkSubprojects = false,
                    createdAt = 1L,
                    updatedAt = 1L,
                    version = 1L,
                    isDeleted = false,
                ),
            )

            initializer(database, repositories).apply(
                workspaceId = workspaceId,
                roleCode = "management",
                now = 20L,
            )

            assertNull(database.contextDao().getContextById(workspaceId))
            assertNull(database.contextStructureDao().getStructureByContext(workspaceId))

            assertEquals(WorkspaceCapabilityState.ACTIVE, repositories.inbox.getState(workspaceId)?.lifecycleState)
            assertTrue(repositories.backlog.getState(workspaceId)?.lifecycleState == WorkspaceCapabilityState.ACTIVE)
            assertFalse(repositories.dashboard.isEnabled(workspaceId))
            assertFalse(repositories.executionLog.isEnabled(workspaceId))
            assertNull(repositories.connections.getState(workspaceId))

            // Management has no Direction role capability. The auto-link preset
            // boolean therefore does not invent a Direction capability.
            assertNull(repositories.direction.getState(workspaceId))
        } finally {
            database.close()
        }
    }

    @Test
    fun `direction role initializes canonical direction configuration`() = runBlocking {
        val database = database()
        try {
            val repositories = repositories(database)
            val workspaceId = "direction-workspace"
            seedStandaloneWorkspace(
                database = database,
                id = workspaceId,
                name = "Direction",
                roleCode = "direction",
                now = 10L,
            )

            database.structurePresetDao().insertPreset(
                ContextRoleProfile(
                    id = "direction-preset",
                    code = "direction",
                    label = "Direction",
                    description = null,
                    enableAutoLinkSubprojects = false,
                    createdAt = 1L,
                    updatedAt = 1L,
                    version = 1L,
                    isDeleted = false,
                ),
            )

            initializer(database, repositories).apply(
                workspaceId = workspaceId,
                roleCode = "direction",
                now = 20L,
            )

            val state = requireNotNull(repositories.direction.getState(workspaceId))
            assertEquals(WorkspaceCapabilityState.ACTIVE, state.lifecycleState)
            assertFalse(state.configuration.autoLinkChildWorkspaces)
            assertNull(database.contextDao().getContextById(workspaceId))
            assertNull(database.contextStructureDao().getStructureByContext(workspaceId))
        } finally {
            database.close()
        }
    }

    @Test
    fun `unknown role creates no implicit capability instances`() = runBlocking {
        val database = database()
        try {
            val repositories = repositories(database)
            val workspaceId = "custom-workspace"
            seedStandaloneWorkspace(
                database = database,
                id = workspaceId,
                name = "Custom",
                roleCode = "unknown-role",
                now = 10L,
            )

            initializer(database, repositories).apply(
                workspaceId = workspaceId,
                roleCode = "unknown-role",
                now = 20L,
            )

            val capabilities =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == workspaceId }

            assertTrue(capabilities.isEmpty())
            assertNull(database.contextDao().getContextById(workspaceId))
            assertNull(database.contextStructureDao().getStructureByContext(workspaceId))
        } finally {
            database.close()
        }
    }

    private suspend fun seedStandaloneWorkspace(
        database: AppDatabase,
        id: String,
        name: String,
        roleCode: String?,
        now: Long,
    ) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = name,
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = roleCode,
                    workspaceOrder = 0L,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.STANDALONE.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private data class Repositories(
        val inbox: CanonicalInboxRepository,
        val backlog: CanonicalBacklogRepository,
        val inboxSorting: CanonicalInboxSortingRepository,
        val keyProblems: CanonicalKeyProblemsRepository,
        val direction: CanonicalDirectionRepository,
        val dashboard: CanonicalDashboardCapabilityRepository,
        val executionLog: CanonicalExecutionLogRepository,
        val connections: CanonicalConnectionsRepository,
    )

    private fun repositories(database: AppDatabase): Repositories {
        val store =
            CanonicalCapabilityInstanceStore(
                database = database,
                workspaceDao = database.workspaceDao(),
                orientationDao = database.orientationDao(),
            )
        val orientationRepository =
            CanonicalOrientationRepository(
                database = database,
                dao = database.orientationDao(),
            )

        return Repositories(
            inbox =
                CanonicalInboxRepository(
                    database = database,
                    instanceStore = store,
                    recordDao = database.workspaceInboxRecordDao(),
                ),
            backlog =
                CanonicalBacklogRepository(
                    database = database,
                    instanceStore = store,
                    entryDao = database.workspaceBacklogEntryDao(),
                    targetValidator = CanonicalBacklogTargetValidator(database),
                ),
            inboxSorting =
                CanonicalInboxSortingRepository(
                    instanceStore = store,
                    orientationDao = database.orientationDao(),
                ),
            keyProblems =
                CanonicalKeyProblemsRepository(
                    database = database,
                    instanceStore = store,
                    problemDao = database.workspaceProblemDao(),
                    workspaceDao = database.workspaceDao(),
                    attachmentDao = database.attachmentDao(),
                ),
            direction =
                CanonicalDirectionRepository(
                    database = database,
                    instanceStore = store,
                    entryDao = database.workspaceDirectionEntryDao(),
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                    orientationRepository = orientationRepository,
                ),
            dashboard =
                CanonicalDashboardCapabilityRepository(
                    instanceStore = store,
                ),
            executionLog =
                CanonicalExecutionLogRepository(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    contextManagementDao = database.contextManagementDao(),
                    instanceStore = store,
                ),
            connections =
                CanonicalConnectionsRepository(
                    database = database,
                    instanceStore = store,
                    connectionDao = database.workspaceConnectionDao(),
                ),
        )
    }

    private fun initializer(
        database: AppDatabase,
        repositories: Repositories,
    ) = CanonicalWorkspaceRolePresetInitializer(
        database = database,
        structurePresetDao = database.structurePresetDao(),
        inboxRepository = repositories.inbox,
        backlogRepository = repositories.backlog,
        inboxSortingRepository = repositories.inboxSorting,
        keyProblemsRepository = repositories.keyProblems,
        directionRepository = repositories.direction,
        dashboardRepository = repositories.dashboard,
        executionLogRepository = repositories.executionLog,
        connectionsRepository = repositories.connections,
    )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
