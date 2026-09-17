package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SystemContextCanonicalBacklogLifecycleAccessRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `non-system is not handled and malformed System ownership fails closed`() = runBlocking {
        val database = database()
        try {
            val access = access(database)
            assertNull(access.getState("ordinary"))
            assertFalse(access.setEnabled("ordinary", true, now = 10L))

            val id = SystemContexts.INBOX.raw
            assertFalse(requireNotNull(access.getState(id)).isCanonicalOwnerAvailable)

            database.workspaceDao().upsert(
                listOf(workspace(id).copy(provenance = WorkspaceProvenance.CONTEXT_BACKED.name, sourceContextId = id)),
            )
            assertFalse(requireNotNull(access.getState(id)).isCanonicalOwnerAvailable)
            database.workspaceDao().upsert(listOf(workspace(id).copy(sourceContextId = "legacy")))
            assertFalse(requireNotNull(access.getState(id)).isCanonicalOwnerAvailable)
            database.workspaceDao().upsert(listOf(workspace(id).copy(isDeleted = true)))
            assertFalse(requireNotNull(access.getState(id)).isCanonicalOwnerAvailable)

            val overrides = canonicalSystemBacklogLifecycleOverrides(id, access.getState(id))
            assertEquals(false, overrides[com.romankozak.forwardappmobile.core.capability.CapabilityId("backlog")])
        } finally {
            database.close()
        }
    }

    @Test
    fun `established lifecycle is canonical and stale compatibility is reconciled`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            val stale = ContextConfiguration.default(id).copy(enableBacklog = false)
            database.contextStructureDao().insertStructure(stale)
            seedCapability(database, id, WorkspaceCapabilityState.ACTIVE)
            val access = access(database)

            assertTrue(requireNotNull(access.getState(id)).enabled)
            assertEquals(true, access.reconcileCompatibilityOutput(stale).enableBacklog)

            listOf(WorkspaceCapabilityState.DISABLED, WorkspaceCapabilityState.ARCHIVED).forEach { lifecycle ->
                val current = database.orientationDao().getAllWorkspaceCapabilities().single()
                database.orientationDao().upsertWorkspaceCapabilities(
                    listOf(current.copy(state = lifecycle.name, version = current.version + 1L)),
                )
                assertFalse(requireNotNull(access.getState(id)).enabled)
                assertEquals(false, access.reconcileCompatibilityOutput(stale.copy(enableBacklog = true)).enableBacklog)
            }

            val current = database.orientationDao().getAllWorkspaceCapabilities().single()
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(current.copy(isDeleted = true, version = current.version + 1L)),
            )
            val deleted = requireNotNull(access.getState(id))
            assertTrue(deleted.isEstablished)
            assertFalse(deleted.enabled)
            assertEquals(false, access.reconcileCompatibilityOutput(stale.copy(enableBacklog = true)).enableBacklog)

            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    current.copy(
                        state = WorkspaceCapabilityState.ACTIVE.name,
                        configurationVersion = 999,
                        configuration = "{}",
                        isDeleted = false,
                        version = current.version + 2L,
                    ),
                ),
            )
            val malformed = requireNotNull(access.getState(id))
            assertTrue(malformed.isEstablished)
            assertFalse(malformed.enabled)
            assertEquals(false, access.reconcileCompatibilityOutput(stale.copy(enableBacklog = true)).enableBacklog)
        } finally {
            database.close()
        }
    }

    @Test
    fun `explicit missing disable establishes durable idempotent canonical state`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(enableBacklog = true),
            )
            val access = access(database)

            assertTrue(access.setEnabled(id, false, now = 10L))
            val established = database.orientationDao().getAllWorkspaceCapabilities().single()
            assertEquals(WorkspaceCapabilityState.DISABLED.name, established.state)
            assertFalse(established.isDeleted)
            assertEquals(false, database.contextStructureDao().getStructureByContext(id)?.enableBacklog)

            assertTrue(access.setEnabled(id, false, now = 20L))
            assertEquals(established, database.orientationDao().getAllWorkspaceCapabilities().single())
            assertEquals(
                false,
                access.reconcileCompatibilityOutput(
                    requireNotNull(database.contextStructureDao().getStructureByContext(id)).copy(enableBacklog = true),
                ).enableBacklog,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `compatibility output failure rolls back canonical lifecycle mutation`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(enableBacklog = false),
            )
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_backlog_lifecycle_compatibility_output
                BEFORE INSERT ON context_structures
                BEGIN
                    SELECT RAISE(ABORT, 'compatibility output rejected');
                END
                """.trimIndent(),
            )

            assertTrue(runCatching { access(database).setEnabled(id, true, now = 20L) }.isFailure)
            assertTrue(database.orientationDao().getAllWorkspaceCapabilities().isEmpty())
            assertEquals(false, database.contextStructureDao().getStructureByContext(id)?.enableBacklog)
        } finally {
            database.close()
        }
    }

    private fun access(database: AppDatabase) =
        SystemContextCanonicalBacklogLifecycleAccess(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextStructureDao = database.contextStructureDao(),
            backlogRepository = repository(database),
        )

    private fun repository(database: AppDatabase) =
        CanonicalBacklogRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(database, database.workspaceDao(), database.orientationDao()),
            entryDao = database.workspaceBacklogEntryDao(),
            targetValidator = CanonicalBacklogTargetValidator(database),
        )

    private suspend fun seedCapability(
        database: AppDatabase,
        workspaceId: String,
        lifecycle: WorkspaceCapabilityState,
    ) {
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                WorkspaceCapabilityInstanceEntity(
                    id = "backlog-$workspaceId",
                    workspaceId = workspaceId,
                    capabilityType = "BACKLOG",
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = lifecycle.name,
                    configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration =
                        BacklogCapabilityConfigurationCodec.encode(
                            BacklogCapabilityConfigurationV2(false),
                        ),
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
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
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
