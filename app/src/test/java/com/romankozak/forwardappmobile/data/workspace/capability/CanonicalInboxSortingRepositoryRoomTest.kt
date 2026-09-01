package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextInboxSortingEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.repository.ContextInboxSortingRepository
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingMode
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingRule
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalInboxSortingRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `typed configuration is stored on stable capability instance`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(workspace()))
            database.orientationDao().upsertWorkspaceCapabilities(listOf(capability()))
            val repository = repository(database)
            val configuration =
                InboxSortingCapabilityConfigurationV1(
                    listOf(
                        WorkspaceSortingRule(WorkspaceSortingTarget.INBOX, WorkspaceSortingMode.ALPHA),
                        WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.OLDEST),
                    ),
                )

            repository.updateConfiguration("owner", configuration, now = 20L)

            assertEquals(configuration, repository.getConfiguration("owner"))
            assertEquals(configuration, repository.observeConfiguration("owner").first())
            val stored = singleCapability(database)
            assertEquals("sorting-owner", stored.id)
            assertEquals(2L, stored.version)
            assertEquals(20L, stored.updatedAt)
            assertNull(stored.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `configuration update is idempotent and requires active policy`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(workspace()))
            database.orientationDao().upsertWorkspaceCapabilities(listOf(capability()))
            val repository = repository(database)
            val default = InboxSortingCapabilityConfigurationV1(emptyList())

            repository.updateConfiguration("owner", default, now = 20L)
            assertEquals(1L, singleCapability(database).version)

            repository.disable("owner", now = 30L)
            val failure =
                runCatching {
                    repository.updateConfiguration(
                        "owner",
                        InboxSortingCapabilityConfigurationV1(
                            listOf(WorkspaceSortingRule(WorkspaceSortingTarget.INBOX, WorkspaceSortingMode.OLDEST)),
                        ),
                        now = 40L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(2L, singleCapability(database).version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `legacy facing settings adapter writes canonical config only`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(workspace()))
            database.orientationDao().upsertWorkspaceCapabilities(listOf(capability()))
            val canonical = repository(database)
            val compatibility = ContextInboxSortingRepository(canonical)

            compatibility.updateRulesText(
                contextId = "owner",
                rulesText = "attachments:type\ninbox:alpha",
            )

            assertEquals(
                "inbox:alpha\nconnections:type",
                compatibility.get("owner").rulesText,
            )
            assertTrue(database.contextInboxSortingDao().getAllRaw().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `old full backup fallback materializes planner output and clears staged evidence`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(workspace()))
            database.orientationDao().upsertWorkspaceCapabilities(listOf(capability()))
            database.contextInboxSortingDao().upsert(
                ContextInboxSortingEntity(
                    contextId = "owner",
                    rulesText = "attachments:type\nbacklog:oldest",
                    updatedAt = 20L,
                ),
            )

            InboxSortingLegacyFullBackupAdapter(
                database = database,
                workspaceDao = database.workspaceDao(),
                contextInboxSortingDao = database.contextInboxSortingDao(),
            ).materializeStagedEvidence()

            assertEquals(
                InboxSortingCapabilityConfigurationV1(
                    listOf(
                        WorkspaceSortingRule(WorkspaceSortingTarget.CONNECTIONS, WorkspaceSortingMode.TYPE),
                        WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.OLDEST),
                    ),
                ),
                repository(database).getConfiguration("owner"),
            )
            assertTrue(database.contextInboxSortingDao().getAllRaw().isEmpty())
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalInboxSortingRepository(
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            orientationDao = database.orientationDao(),
        )

    private suspend fun singleCapability(database: AppDatabase): WorkspaceCapabilityInstanceEntity =
        database.orientationDao().getAllWorkspaceCapabilities().single()

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
            provenance = "CONTEXT_BACKED",
            sourceContextId = "owner",
        )

    private fun capability() =
        WorkspaceCapabilityInstanceEntity(
            id = "sorting-owner",
            workspaceId = "owner",
            capabilityType = "INBOX_SORTING",
            instanceKey = "default",
            capabilityOrder = 1L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{\"rules\":[]}",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = 10L,
            isDeleted = false,
            version = 1L,
        )
}
