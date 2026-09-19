package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.SystemRemainingCapabilityLifecycleState
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.ConnectionsCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.DirectionCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.InboxCapabilityState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextScreenDataMapperTest {
    @Test
    fun `legacy note placement remains visible with content identity after canonical cutover`() {
        val note =
            LegacyNoteEntity(
                id = "note-content",
                contextId = "owner",
                title = "Historical note",
                content = "Preserved content",
            )
        val placement =
            BacklogItem(
                id = "canonical-placement",
                contextId = "owner",
                itemType = BacklogItemTypeValues.NOTE,
                entityId = note.id,
                order = 0L,
            )

        val loaded =
            ContextScreenDataMapper().map(
                contextId = "owner",
                snapshot = emptySnapshot(rawItems = listOf(placement), notes = listOf(note)),
            )

        val item = loaded.items.single() as BacklogItemContent.NoteItem
        assertSame(note, item.note)
        assertEquals("canonical-placement", item.backlogItem.id)
        assertEquals("note-content", item.backlogItem.entityId)
    }

    @Test
    fun `shell-free linked project is materialized from canonical presentation`() {
        val linkedId = "shell-free-linked-project"
        val placement =
            BacklogItem(
                id = "linked-placement",
                contextId = "owner",
                itemType = BacklogItemTypeValues.SUBLIST,
                entityId = linkedId,
                order = 7L,
            )
        val presentation =
            ContextPresentation(
                id = linkedId,
                name = "Canonical linked title",
                description = "Canonical linked description",
                parentId = "canonical-parent",
                roleCode = "canonical-role",
                order = 42L,
                tags = listOf("canonical-tag"),
            )

        val loaded =
            ContextScreenDataMapper().map(
                contextId = "owner",
                snapshot =
                    emptySnapshot(
                        rawItems = listOf(placement),
                        presentationUniverse = listOf(presentation),
                    ),
            )

        val item = loaded.items.single() as BacklogItemContent.ContextLinkItem

        assertEquals(linkedId, item.project.id)
        assertEquals("Canonical linked title", item.project.name)
        assertEquals("Canonical linked description", item.project.description)
        assertEquals("canonical-parent", item.project.parentId)
        assertEquals("canonical-role", item.project.roleCode)
        assertEquals(42L, item.project.order)
        assertEquals(listOf("canonical-tag"), item.project.tags)
        assertEquals(null, item.legacyProject)
        assertEquals("linked-placement", item.backlogItem.id)
    }

    @Test
    fun `System Inbox and Direction projection fails closed on disabled or archived canonical state`() {
        val id = SystemContexts.INBOX.raw
        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState =
                            systemState(
                                inboxLifecycle = WorkspaceCapabilityState.DISABLED,
                                directionLifecycle = WorkspaceCapabilityState.ARCHIVED,
                            ),
                    ),
            )

        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("inbox")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("direction")])
    }

    @Test
    fun `unavailable System Inbox fails closed without hiding healthy Direction`() {
        val id = SystemContexts.INBOX.raw
        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState = SystemInboxDirectionState(null, directionState()),
                    ),
            )

        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("inbox")])
        assertEquals(true, loaded.canonicalCapabilityOverrides[CapabilityId("direction")])
    }

    @Test
    fun `deleted System capability fails closed`() {
        val id = SystemContexts.INBOX.raw
        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState =
                            SystemInboxDirectionState(
                                inbox = inboxState(isDeleted = true),
                                direction = null,
                            ),
                    ),
            )

        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("inbox")])
    }

    @Test
    fun `active valid System Inbox and Direction override legacy runtime state`() {
        val id = SystemContexts.INBOX.raw
        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState = systemState(),
                    ),
            )

        assertEquals(true, loaded.canonicalCapabilityOverrides[CapabilityId("inbox")])
        assertEquals(true, loaded.canonicalCapabilityOverrides[CapabilityId("direction")])
    }

    @Test
    fun `canonical ordinary Workspace capability rows replace contradictory legacy configuration`() {
        val id = "canonical-ordinary"
        val presentation =
            ContextPresentation(
                id = id,
                name = "Canonical ordinary",
                description = null,
                parentId = null,
                roleCode = null,
                order = 0L,
                tags = emptyList(),
            )
        val legacyConfig =
            ContextConfiguration.default(id).copy(
                enableInbox = true,
                enableBacklog = false,
                enableDashboard = true,
                enableAttachments = true,
                experimentalCapabilityIds = listOf(CapabilityId("direction")),
            )
        val canonicalBacklog =
            WorkspaceCapabilityInstanceEntity(
                id = "backlog-$id",
                workspaceId = id,
                capabilityType = WorkspaceCapabilityType.BACKLOG.name,
                instanceKey = "default",
                capabilityOrder = 0L,
                state = WorkspaceCapabilityState.ACTIVE.name,
                configurationVersion = 1,
                configuration = "{}",
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            )

        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot =
                    emptySnapshot(
                        config = legacyConfig,
                        workspaceCapabilities = listOf(canonicalBacklog),
                        presentation = presentation,
                        hasCanonicalWorkspaceOwner = true,
                    ),
            )

        assertEquals(true, loaded.canonicalCapabilityOverrides[CapabilityId("backlog")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("inbox")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("dashboard")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("connections")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("direction")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("log")])
        assertTrue(loaded.suppressPresetCapabilityDerivation)
    }

    @Test
    fun `live compatibility Context cannot veto canonical Workspace capability authority`() {
        val id = "canonical-with-live-context"
        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot =
                    emptySnapshot(
                        context =
                            Context(
                                id = id,
                                name = "Legacy shell",
                                description = null,
                                parentId = null,
                                createdAt = 1L,
                                updatedAt = 1L,
                            ),
                        config =
                            ContextConfiguration.default(id).copy(
                                enableBacklog = false,
                                experimentalCapabilityIds = emptyList(),
                            ),
                        presentation =
                            ContextPresentation(
                                id = id,
                                name = "Canonical owner",
                                description = null,
                                parentId = null,
                                roleCode = null,
                                order = 0L,
                                tags = emptyList(),
                            ),
                        hasCanonicalWorkspaceOwner = true,
                        workspaceCapabilities = listOf(activeBacklog(id)),
                    ),
            )

        assertEquals(true, loaded.canonicalCapabilityOverrides[CapabilityId("backlog")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("inbox")])
        assertTrue(loaded.suppressPresetCapabilityDerivation)
    }

    @Test
    fun `ordinary Context receives no System canonical overrides`() {
        val loaded =
            ContextScreenDataMapper().map(
                contextId = "ordinary",
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState =
                            systemState(inboxLifecycle = WorkspaceCapabilityState.DISABLED),
                    ),
            )

        assertEquals(emptyMap<CapabilityId, Boolean>(), loaded.canonicalCapabilityOverrides)
    }

    @Test
    fun `remaining System capabilities replace contradictory legacy runtime state`() {
        val id = SystemContexts.INBOX.raw
        val state =
            SystemRemainingCapabilityLifecycleState(
                connections =
                    ConnectionsCapabilityState(
                        WorkspaceCapabilityState.DISABLED,
                        false,
                        ConnectionsCapabilityConfigurationV1,
                    ),
                inboxSorting = null,
                keyProblems = null,
                connectionsEstablished = true,
                inboxSortingEstablished = true,
                keyProblemsEstablished = true,
            )

        val loaded =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot = emptySnapshot(systemRemainingCapabilityState = state),
            )

        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("connections")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("inbox_sorting")])
        assertEquals(false, loaded.canonicalCapabilityOverrides[CapabilityId("key_problems")])
    }

    @Test
    fun `established System Backlog lifecycle is a canonical override including disabled`() {
        val id = SystemContexts.INBOX.raw
        val disabled =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot = emptySnapshot(systemBacklogLifecycleState = backlogState(WorkspaceCapabilityState.DISABLED)),
            )
        val active =
            ContextScreenDataMapper().map(
                contextId = id,
                snapshot = emptySnapshot(systemBacklogLifecycleState = backlogState(WorkspaceCapabilityState.ACTIVE)),
            )

        assertEquals(false, disabled.canonicalCapabilityOverrides[CapabilityId("backlog")])
        assertEquals(true, active.canonicalCapabilityOverrides[CapabilityId("backlog")])
    }

    @Test
    fun `ordinary Context receives no System Backlog override`() {
        val loaded =
            ContextScreenDataMapper().map(
                contextId = "ordinary",
                snapshot = emptySnapshot(systemBacklogLifecycleState = backlogState(WorkspaceCapabilityState.DISABLED)),
            )

        assertEquals(null, loaded.canonicalCapabilityOverrides[CapabilityId("backlog")])
    }

    @Test
    fun `valid promoted System projection marks preset derivation as runtime inert`() {
        val loaded =
            ContextScreenDataMapper().map(
                contextId = SystemContexts.INBOX.raw,
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState = systemState(),
                        systemRemainingCapabilityState = availableRemainingState(),
                        systemBacklogLifecycleState = backlogState(WorkspaceCapabilityState.ACTIVE),
                    ),
            )

        assertTrue(loaded.suppressPresetCapabilityDerivation)
    }

    @Test
    fun `ordinary or malformed System projection retains bounded legacy seed mode`() {
        val ordinary =
            ContextScreenDataMapper().map(
                contextId = "ordinary",
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState = systemState(),
                        systemRemainingCapabilityState = availableRemainingState(),
                        systemBacklogLifecycleState = backlogState(WorkspaceCapabilityState.ACTIVE),
                    ),
            )
        val malformedSystem =
            ContextScreenDataMapper().map(
                contextId = SystemContexts.INBOX.raw,
                snapshot =
                    emptySnapshot(
                        systemInboxDirectionState =
                            SystemInboxDirectionState(null, null, isCanonicalOwnerAvailable = false),
                        systemRemainingCapabilityState = availableRemainingState(),
                        systemBacklogLifecycleState = backlogState(WorkspaceCapabilityState.ACTIVE),
                    ),
            )

        assertFalse(ordinary.suppressPresetCapabilityDerivation)
        assertFalse(malformedSystem.suppressPresetCapabilityDerivation)
    }

    private fun emptySnapshot(
        rawItems: List<BacklogItem> = emptyList(),
        notes: List<LegacyNoteEntity> = emptyList(),
        presentationUniverse: List<ContextPresentation> = emptyList(),
        config: ContextConfiguration? = null,
        context: Context? = null,
        workspaceCapabilities: List<WorkspaceCapabilityInstanceEntity> = emptyList(),
        hasCanonicalWorkspaceOwner: Boolean = false,
        presentation: ContextPresentation? = null,
        systemInboxDirectionState: SystemInboxDirectionState? = null,
        systemRemainingCapabilityState: SystemRemainingCapabilityLifecycleState? = null,
        systemBacklogLifecycleState: SystemBacklogLifecycleState? = null,
    ): ContextScreenDataSnapshot =
        ContextScreenDataSnapshot(
            context = context,
            rawItems = rawItems,
            config = config,
            logs = emptyList(),
            checklists = emptyList(),
            noteDocuments = emptyList(),
            musicNotes = emptyList(),
            directionItems = emptyList(),
            attachments = emptyList(),
            linkItems = emptyList(),
            reminders = emptyList(),
            recentItems = emptyList(),
            notes = notes,
            goals = emptyList(),
            hasCanonicalWorkspaceOwner = hasCanonicalWorkspaceOwner,
            workspaceCapabilities = workspaceCapabilities,
            systemInboxDirectionState = systemInboxDirectionState,
            systemRemainingCapabilityState = systemRemainingCapabilityState,
            systemBacklogLifecycleState = systemBacklogLifecycleState,
            presentation = presentation,
            presentationUniverse = presentationUniverse,
        )

    private fun activeBacklog(workspaceId: String) =
        WorkspaceCapabilityInstanceEntity(
            id = "backlog-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = WorkspaceCapabilityType.BACKLOG.name,
            instanceKey = "default",
            capabilityOrder = 0L,
            state = WorkspaceCapabilityState.ACTIVE.name,
            configurationVersion = 1,
            configuration = "{}",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun backlogState(state: WorkspaceCapabilityState) =
        SystemBacklogLifecycleState(
            backlog =
                BacklogCapabilityState(
                    lifecycleState = state,
                    isDeleted = false,
                    configuration = BacklogCapabilityConfigurationV2(false),
                ),
            isEstablished = true,
        )

    private fun availableRemainingState() =
        SystemRemainingCapabilityLifecycleState(
            connections = null,
            inboxSorting = null,
            keyProblems = null,
            connectionsEstablished = false,
            inboxSortingEstablished = false,
            keyProblemsEstablished = false,
        )

    private fun systemState(
        inboxLifecycle: WorkspaceCapabilityState = WorkspaceCapabilityState.ACTIVE,
        directionLifecycle: WorkspaceCapabilityState = WorkspaceCapabilityState.ACTIVE,
    ) = SystemInboxDirectionState(inboxState(inboxLifecycle), directionState(directionLifecycle))

    private fun inboxState(
        state: WorkspaceCapabilityState = WorkspaceCapabilityState.ACTIVE,
        isDeleted: Boolean = false,
    ) =
        InboxCapabilityState(
            state,
            isDeleted,
            InboxCapabilityConfigurationV1(InboxOwnerVisibility.KEEP_VISIBLE),
        )

    private fun directionState(
        state: WorkspaceCapabilityState = WorkspaceCapabilityState.ACTIVE,
        isDeleted: Boolean = false,
    ) = DirectionCapabilityState(state, isDeleted, DirectionCapabilityConfigurationV1(true))
}
