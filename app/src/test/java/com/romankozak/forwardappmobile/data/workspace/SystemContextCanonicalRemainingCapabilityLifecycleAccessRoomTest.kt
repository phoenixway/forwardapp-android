package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
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
class SystemContextCanonicalRemainingCapabilityLifecycleAccessRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `explicit missing disable establishes durable idempotent canonical state`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(
                    enableAttachments = true,
                    experimentalCapabilityIds =
                        listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                ),
            )
            val fixture = fixture(database)

            assertTrue(fixture.access.setConnectionsEnabled(id, false, now = 10L))
            assertTrue(fixture.access.setInboxSortingEnabled(id, false, now = 11L))
            assertTrue(fixture.access.setKeyProblemsEnabled(id, false, now = 12L))
            val first = capabilities(database, id)
            REMAINING_TYPES.forEach { type ->
                assertEquals(WorkspaceCapabilityState.DISABLED.name, first.getValue(type).state)
            }

            fixture.access.setConnectionsEnabled(id, false, now = 20L)
            fixture.access.setInboxSortingEnabled(id, false, now = 21L)
            fixture.access.setKeyProblemsEnabled(id, false, now = 22L)
            val repeated = capabilities(database, id)
            REMAINING_TYPES.forEach { type ->
                assertEquals(first.getValue(type).version, repeated.getValue(type).version)
            }

            val stale =
                requireNotNull(database.contextStructureDao().getStructureByContext(id)).copy(
                    enableAttachments = true,
                    experimentalCapabilityIds =
                        listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                    version = 99L,
                )
            val reconciled = fixture.access.reconcileCompatibilityOutput(stale)
            database.contextStructureDao().insertStructure(reconciled)
            val finalRows = capabilities(database, id)
            REMAINING_TYPES.forEach { type ->
                assertEquals(WorkspaceCapabilityState.DISABLED.name, finalRows.getValue(type).state)
                assertEquals(repeated.getValue(type).version, finalRows.getValue(type).version)
            }
            val output = requireNotNull(database.contextStructureDao().getStructureByContext(id))
            assertFalse(output.enableAttachments == true)
            assertFalse(CapabilityId("inbox_sorting") in output.experimentalCapabilityIds)
            assertFalse(CapabilityId("key_problems") in output.experimentalCapabilityIds)
        } finally {
            database.close()
        }
    }

    @Test
    fun `established lifecycle wins and compatibility output preserves unrelated fields`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            val fixture = fixture(database)
            fixture.connections.enable(id, now = 10L)
            fixture.inboxSorting.enable(id, now = 11L)
            fixture.inboxSorting.archive(id, now = 12L)
            fixture.keyProblems.enable(id, now = 13L)
            fixture.keyProblems.deleteCapability(id, now = 14L)

            val candidate =
                ContextConfiguration.default(id).copy(
                    basePresetCode = "preserve",
                    enableAttachments = false,
                    experimentalCapabilityIds =
                        listOf(
                            CapabilityId("unrelated"),
                            CapabilityId("inbox_sorting"),
                            CapabilityId("key_problems"),
                        ),
                )
            val output = fixture.access.reconcileCompatibilityOutput(candidate)

            assertTrue(output.enableAttachments == true)
            assertTrue(CapabilityId("unrelated") in output.experimentalCapabilityIds)
            assertFalse(CapabilityId("inbox_sorting") in output.experimentalCapabilityIds)
            assertFalse(CapabilityId("key_problems") in output.experimentalCapabilityIds)
            assertEquals("preserve", output.basePresetCode)
            val state = requireNotNull(fixture.access.getState(id))
            assertTrue(state.connectionsEnabled)
            assertFalse(state.inboxSortingEnabled)
            assertFalse(state.keyProblemsEnabled)
            assertTrue(state.inboxSortingEstablished)
            assertTrue(state.keyProblemsEstablished)
        } finally {
            database.close()
        }
    }

    @Test
    fun `non-system is not handled and malformed System ownership fails closed`() = runBlocking {
        val database = database()
        try {
            val access = fixture(database).access
            assertFalse(access.handles("ordinary"))
            assertNull(access.getState("ordinary"))

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
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_remaining_system_compatibility_output
                BEFORE INSERT ON context_structures
                BEGIN
                    SELECT RAISE(ABORT, 'compatibility output rejected');
                END
                """.trimIndent(),
            )

            val result = runCatching { fixture(database).access.setConnectionsEnabled(id, true, now = 10L) }

            assertTrue(result.isFailure)
            assertTrue(capabilities(database, id).isEmpty())
            assertNull(database.contextStructureDao().getStructureByContext(id))
        } finally {
            database.close()
        }
    }

    private fun fixture(database: AppDatabase): Fixture {
        val store = CanonicalCapabilityInstanceStore(database, database.workspaceDao(), database.orientationDao())
        val connections = CanonicalConnectionsRepository(database, store, database.workspaceConnectionDao())
        val inboxSorting = CanonicalInboxSortingRepository(store, database.orientationDao())
        val keyProblems =
            CanonicalKeyProblemsRepository(
                database,
                store,
                database.workspaceProblemDao(),
                database.workspaceDao(),
                database.attachmentDao(),
            )
        val access =
            SystemContextCanonicalRemainingCapabilityLifecycleAccess(
                database,
                database.workspaceDao(),
                database.contextStructureDao(),
                connections,
                inboxSorting,
                keyProblems,
            )
        return Fixture(
            access = access,
            connections = connections,
            inboxSorting = inboxSorting,
            keyProblems = keyProblems,
        )
    }

    private suspend fun capabilities(database: AppDatabase, workspaceId: String) =
        database.orientationDao().getAllWorkspaceCapabilities()
            .filter { it.workspaceId == workspaceId }
            .associateBy { WorkspaceCapabilityType.valueOf(it.capabilityType) }

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

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private data class Fixture(
        val access: SystemContextCanonicalRemainingCapabilityLifecycleAccess,
        val connections: CanonicalConnectionsRepository,
        val inboxSorting: CanonicalInboxSortingRepository,
        val keyProblems: CanonicalKeyProblemsRepository,
    )

    private companion object {
        val REMAINING_TYPES =
            setOf(
                WorkspaceCapabilityType.CONNECTIONS,
                WorkspaceCapabilityType.INBOX_SORTING,
                WorkspaceCapabilityType.KEY_PROBLEMS,
            )
    }
}
