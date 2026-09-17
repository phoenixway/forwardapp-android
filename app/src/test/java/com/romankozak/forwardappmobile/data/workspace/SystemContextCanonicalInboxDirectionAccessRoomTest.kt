package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
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
class SystemContextCanonicalInboxDirectionAccessRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `non-system identity is not applicable`() = runBlocking {
        val database = database()
        try {
            val access = access(database)
            assertFalse(access.handles("ordinary"))
            assertNull(access.getState("ordinary"))
            assertNull(access.directionAutoLinkEnabled("ordinary"))
            assertFalse(access.setInboxEnabled("ordinary", true, now = 10L))
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical commands win and update only bounded legacy compatibility fields`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            val legacy =
                ContextConfiguration.default(id).copy(
                    basePresetCode = "preserve-role",
                    enableInbox = false,
                    experimentalCapabilityIds = listOf(CapabilityId("unrelated")),
                    removeInboxEntryAfterTagAutocopy = false,
                    enableAutoLinkSubprojects = true,
                )
            database.contextStructureDao().insertStructure(legacy)
            val access = access(database)

            assertTrue(access.setInboxEnabled(id, true, now = 10L))
            access.updateInboxConfiguration(
                id,
                InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED),
                now = 11L,
            )
            assertTrue(access.setDirectionEnabled(id, true, now = 12L))
            access.updateDirectionConfiguration(
                id,
                DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false),
                now = 13L,
            )

            val state = requireNotNull(access.getState(id))
            assertTrue(state.isCanonicalOwnerAvailable)
            assertTrue(state.inboxEnabled)
            assertTrue(state.directionEnabled)
            assertEquals(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED, state.inbox?.configuration?.ownerVisibility)
            assertFalse(requireNotNull(state.direction).configuration.autoLinkChildWorkspaces)
            assertEquals(false, access.directionAutoLinkEnabled(id))
            val output = requireNotNull(database.contextStructureDao().getStructureByContext(id))
            assertTrue(output.enableInbox == true)
            assertTrue(CapabilityId("direction") in output.experimentalCapabilityIds)
            assertTrue(CapabilityId("unrelated") in output.experimentalCapabilityIds)
            assertEquals("preserve-role", output.basePresetCode)
            assertTrue(output.removeInboxEntryAfterTagAutocopy == true)
            assertFalse(output.enableAutoLinkSubprojects == true)
            val canonicalRows = database.orientationDao().getAllWorkspaceCapabilities().associateBy { it.capabilityType }
            assertEquals(2L, canonicalRows.getValue("INBOX").version)
            assertEquals(2L, canonicalRows.getValue("DIRECTION").version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `failed canonical command leaves legacy compatibility unchanged`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            val legacy = ContextConfiguration.default(id).copy(enableInbox = true)
            database.contextStructureDao().insertStructure(legacy)
            val access = access(database)
            access.setInboxEnabled(id, true, now = 10L)
            val inbox = repositories(database).first
            inbox.archive(id, now = 11L)
            val before = requireNotNull(database.contextStructureDao().getStructureByContext(id))

            assertTrue(runCatching { access.setInboxEnabled(id, false, now = 12L) }.isFailure)
            assertEquals(before, database.contextStructureDao().getStructureByContext(id))
            assertEquals(WorkspaceCapabilityState.ARCHIVED, access.getState(id)?.inbox?.lifecycleState)
            assertFalse(requireNotNull(access.getState(id)).inboxEnabled)
        } finally {
            database.close()
        }
    }

    @Test
    fun `non-canonical System Workspace fails closed`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(
                listOf(workspace(id).copy(provenance = WorkspaceProvenance.CONTEXT_BACKED.name, sourceContextId = id)),
            )
            assertClosed(access(database).getState(id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `deleted or legacy-referencing canonical System Workspace fails closed`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id).copy(isDeleted = true)))
            assertClosed(access(database).getState(id))

            database.workspaceDao().upsert(listOf(workspace(id).copy(sourceContextId = id)))
            assertClosed(access(database).getState(id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `missing System Workspace settings read fails closed without legacy fallback`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(
                    enableInbox = true,
                    experimentalCapabilityIds = listOf(CapabilityId("direction")),
                ),
            )

            assertClosed(access(database).getState(id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `compatibility output failure rolls back preceding canonical mutation`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_system_compatibility_output
                BEFORE INSERT ON context_structures
                BEGIN
                    SELECT RAISE(ABORT, 'compatibility output rejected');
                END
                """.trimIndent(),
            )

            assertTrue(runCatching { access(database).setInboxEnabled(id, true, now = 10L) }.isFailure)
            assertTrue(
                database.orientationDao().getAllWorkspaceCapabilities().none {
                    it.workspaceId == id && it.capabilityType == "INBOX"
                },
            )
            assertNull(database.contextStructureDao().getStructureByContext(id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `runtime observation fails closed for every malformed System Workspace ownership shape`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            val access = access(database)

            assertClosed(access.observeState(id).first())

            database.workspaceDao().upsert(
                listOf(workspace(id).copy(provenance = WorkspaceProvenance.CONTEXT_BACKED.name, sourceContextId = id)),
            )
            assertClosed(access.observeState(id).first())

            database.workspaceDao().upsert(listOf(workspace(id).copy(isDeleted = true)))
            assertClosed(access.observeState(id).first())

            database.workspaceDao().upsert(listOf(workspace(id).copy(sourceContextId = "legacy-source")))
            assertClosed(access.observeState(id).first())
        } finally {
            database.close()
        }
    }

    @Test
    fun `reactive state isolates malformed capability and recovers after correction`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(listOf(workspace(id)))
            val (inbox, direction) = repositories(database)
            inbox.enable(id, now = 10L)
            direction.enable(id, now = 11L)
            val access = access(database)
            val sawInitial = CompletableDeferred<Unit>()
            val sawMalformed = CompletableDeferred<Unit>()
            val sawRecovered = CompletableDeferred<Unit>()
            var malformedSeen = false

            val job = launch {
                access.observeState(id).collect { state ->
                    when {
                        state != null && state.inboxEnabled && state.directionEnabled && !malformedSeen ->
                            sawInitial.complete(Unit)
                        state != null && state.inbox == null && state.directionEnabled -> {
                            malformedSeen = true
                            sawMalformed.complete(Unit)
                        }
                        state != null && malformedSeen && state.inboxEnabled && state.directionEnabled ->
                            sawRecovered.complete(Unit)
                    }
                }
            }
            try {
                withTimeout(5_000L) { sawInitial.await() }
                val inboxRow =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.workspaceId == id && it.capabilityType == "INBOX"
                    }
                database.orientationDao().upsertWorkspaceCapabilities(
                    listOf(inboxRow.copy(configurationVersion = 999, version = inboxRow.version + 1L)),
                )
                withTimeout(5_000L) { sawMalformed.await() }
                database.orientationDao().upsertWorkspaceCapabilities(
                    listOf(inboxRow.copy(version = inboxRow.version + 2L, updatedAt = 20L)),
                )
                withTimeout(5_000L) { sawRecovered.await() }
            } finally {
                job.cancelAndJoin()
            }
        } finally {
            database.close()
        }
    }

    private fun assertClosed(state: SystemInboxDirectionState?) {
        val present = requireNotNull(state)
        assertFalse(present.isCanonicalOwnerAvailable)
        assertNull(present.inbox)
        assertNull(present.direction)
        assertFalse(present.inboxEnabled)
        assertFalse(present.directionEnabled)
    }

    private fun access(database: AppDatabase): SystemContextCanonicalInboxDirectionAccess {
        val (inbox, direction) = repositories(database)
        return SystemContextCanonicalInboxDirectionAccess(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextStructureDao = database.contextStructureDao(),
            inboxRepository = inbox,
            directionRepository = direction,
        )
    }

    private fun repositories(database: AppDatabase): Pair<CanonicalInboxRepository, CanonicalDirectionRepository> {
        val store = CanonicalCapabilityInstanceStore(database, database.workspaceDao(), database.orientationDao())
        return CanonicalInboxRepository(database, store, database.workspaceInboxRecordDao()) to
            CanonicalDirectionRepository(
                database = database,
                instanceStore = store,
                entryDao = database.workspaceDirectionEntryDao(),
                workspaceDao = database.workspaceDao(),
                orientationDao = database.orientationDao(),
                orientationRepository = CanonicalOrientationRepository(database, database.orientationDao()),
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

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
