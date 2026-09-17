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
class SystemContextCanonicalBacklogConfigurationAccessRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `non-system is not handled and malformed System ownership fails closed`() = runBlocking {
        val database = database()
        try {
            val access = access(database)
            assertNull(access.getState("ordinary"))
            assertFalse(access.setRemoveEntryAfterTagAutocopy("ordinary", true, now = 10L))

            val id = SystemContexts.INBOX.raw
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(removeBacklogEntryAfterTagAutocopy = true),
            )
            val missing = requireNotNull(access.getState(id))
            assertFalse(missing.isCanonicalOwnerAvailable)
            assertFalse(missing.removeEntryAfterTagAutocopy)

            database.workspaceDao().upsert(
                listOf(workspace(id).copy(provenance = WorkspaceProvenance.CONTEXT_BACKED.name, sourceContextId = id)),
            )
            val contextBacked = requireNotNull(access.getState(id))
            assertFalse(contextBacked.isCanonicalOwnerAvailable)
            assertFalse(contextBacked.removeEntryAfterTagAutocopy)

            database.workspaceDao().upsert(listOf(workspace(id).copy(isDeleted = true)))
            assertFalse(requireNotNull(access.getState(id)).isCanonicalOwnerAvailable)
            database.workspaceDao().upsert(listOf(workspace(id).copy(sourceContextId = "legacy-source")))
            assertFalse(requireNotNull(access.getState(id)).isCanonicalOwnerAvailable)

            database.workspaceDao().upsert(listOf(workspace(id)))
            seedCapability(database, id, removeAfterAutocopy = true)
            val malformed = database.orientationDao().getAllWorkspaceCapabilities().single()
                .copy(configurationVersion = 999, configuration = "{}")
            database.orientationDao().upsertWorkspaceCapabilities(listOf(malformed))
            val malformedState = requireNotNull(access.getState(id))
            assertTrue(malformedState.isCanonicalOwnerAvailable)
            assertTrue(malformedState.isEstablished)
            assertFalse(malformedState.removeEntryAfterTagAutocopy)
        } finally {
            database.close()
        }
    }

    @Test
    fun `established v2 is authority and stale compatibility candidate is reconciled`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            seedCapability(database, id, removeAfterAutocopy = true)
            val legacy =
                ContextConfiguration.default(id).copy(
                    removeBacklogEntryAfterTagAutocopy = false,
                    enableInbox = true,
                )
            database.contextStructureDao().insertStructure(legacy)
            val access = access(database)

            assertTrue(requireNotNull(access.getState(id)).removeEntryAfterTagAutocopy)
            val reconciled = access.reconcileCompatibilityOutput(legacy)
            assertTrue(reconciled.removeBacklogEntryAfterTagAutocopy == true)
            assertTrue(reconciled.enableInbox == true)

            assertTrue(access.setRemoveEntryAfterTagAutocopy(id, false, now = 20L))
            assertFalse(requireNotNull(access.getState(id)).removeEntryAfterTagAutocopy)
            assertFalse(
                requireNotNull(database.contextStructureDao().getStructureByContext(id))
                    .removeBacklogEntryAfterTagAutocopy == true,
            )
            val contradictory = legacy.copy(removeBacklogEntryAfterTagAutocopy = true)
            assertFalse(
                access.reconcileCompatibilityOutput(contradictory)
                    .removeBacklogEntryAfterTagAutocopy == true,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `compatibility output failure rolls back canonical configuration mutation`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            seedCapability(database, id, removeAfterAutocopy = false)
            database.contextStructureDao().insertStructure(ContextConfiguration.default(id))
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_backlog_compatibility_output
                BEFORE INSERT ON context_structures
                BEGIN
                    SELECT RAISE(ABORT, 'compatibility output rejected');
                END
                """.trimIndent(),
            )

            assertTrue(
                runCatching {
                    access(database).setRemoveEntryAfterTagAutocopy(id, true, now = 20L)
                }.isFailure,
            )
            val capability = database.orientationDao().getAllWorkspaceCapabilities().single()
            assertEquals(
                BacklogCapabilityConfigurationV2(false),
                BacklogCapabilityConfigurationCodec.decode(
                    capability.configurationVersion,
                    capability.configuration,
                ),
            )
        } finally {
            database.close()
        }
    }

    private fun access(database: AppDatabase) =
        SystemContextCanonicalBacklogConfigurationAccess(
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
        removeAfterAutocopy: Boolean,
    ) {
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                WorkspaceCapabilityInstanceEntity(
                    id = "backlog-$workspaceId",
                    workspaceId = workspaceId,
                    capabilityType = "BACKLOG",
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = WorkspaceCapabilityState.ACTIVE.name,
                    configurationVersion = 2,
                    configuration =
                        BacklogCapabilityConfigurationCodec.encode(
                            BacklogCapabilityConfigurationV2(removeAfterAutocopy),
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
