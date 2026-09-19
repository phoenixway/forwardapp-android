package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextInboxSortingSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingMode
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingTarget
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotRestoreCanonicalizerTest {
    private val subject = SnapshotRestoreCanonicalizerImpl()

    @Test
    fun `pre canonical ordinary owner content becomes canonical restore payload`() {
        val bundle =
            SnapshotBundle(
                version = 2,
                exportedAt = 100L,
                contexts = listOf(context(OWNER_ID)),
                notes = listOf(note(NOTE_ID, OWNER_ID)),
                backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                inbox = listOf(inbox(OWNER_ID)),
            )

        val result = subject.canonicalize(bundle)

        val workspace = requireNotNull(result.workspaces).single()
        assertEquals(OWNER_ID, workspace.id)
        assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
        assertEquals(null, workspace.sourceContextId)
        assertFalse(workspace.isDeleted)
        assertTrue(result.contexts.single().isDeleted)
        val backlog = requireNotNull(result.workspaceBacklogEntries).single()
        val inbox = requireNotNull(result.workspaceInboxRecords).single()
        assertEquals(BACKLOG_ID, backlog.id)
        assertEquals(OWNER_ID, backlog.workspaceId)
        assertEquals(NOTE_ID, backlog.targetId)
        assertEquals(INBOX_ID, inbox.id)
        assertEquals(OWNER_ID, inbox.workspaceId)
        val types = requireNotNull(result.workspaceCapabilityInstances).map { it.capabilityType }.toSet()
        assertTrue(WorkspaceCapabilityType.BACKLOG.name in types)
        assertTrue(WorkspaceCapabilityType.INBOX.name in types)
    }

    @Test
    fun `irrelevant deleted Context does not require a BACKLOG anchor`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts =
                        listOf(
                            context(OWNER_ID),
                            context("deleted-unused", isDeleted = true),
                        ),
                    notes = listOf(note(NOTE_ID, OWNER_ID)),
                    backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                ),
            )

        val capabilities = requireNotNull(result.workspaceCapabilityInstances)

        assertTrue(
            capabilities.none {
                it.workspaceId == "deleted-unused" &&
                    it.capabilityType == WorkspaceCapabilityType.BACKLOG.name
            },
        )
        assertEquals(
            OWNER_ID,
            requireNotNull(result.workspaceBacklogEntries).single().workspaceId,
        )
    }

    @Test
    fun `deleted legacy BACKLOG owner is preserved as canonical tombstones`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID, isDeleted = true)),
                    notes = listOf(note(NOTE_ID, OWNER_ID)),
                    backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                ),
            )

        val workspace = requireNotNull(result.workspaces).single()
        assertTrue(workspace.isDeleted)

        val capability =
            requireNotNull(result.workspaceCapabilityInstances)
                .single {
                    it.workspaceId == OWNER_ID &&
                        it.capabilityType == WorkspaceCapabilityType.BACKLOG.name
                }
        assertTrue(capability.isDeleted)
        assertEquals(WorkspaceCapabilityState.DISABLED.name, capability.state)

        val backlog = requireNotNull(result.workspaceBacklogEntries).single()
        assertEquals(OWNER_ID, backlog.workspaceId)
        assertEquals(capability.id, backlog.capabilityInstanceId)
        assertTrue(backlog.isDeleted)
    }

    @Test
    fun `legacy inbox order is normalized per Workspace into canonical dense order`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    inbox =
                        listOf(
                            inbox(OWNER_ID).copy(
                                id = "late",
                                order = 40L,
                                createdAt = 30L,
                            ),
                            inbox(OWNER_ID).copy(
                                id = "early-b",
                                order = -12L,
                                createdAt = 20L,
                            ),
                            inbox(OWNER_ID).copy(
                                id = "early-a",
                                order = -12L,
                                createdAt = 10L,
                            ),
                        ),
                ),
            )

        val records = requireNotNull(result.workspaceInboxRecords)
        assertEquals(
            mapOf(
                "early-a" to 0L,
                "early-b" to 1L,
                "late" to 2L,
            ),
            records.associate { it.id to it.order },
        )
    }

    @Test
    fun `explicit canonical empty content remains authoritative`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    notes = listOf(note(NOTE_ID, OWNER_ID)),
                    backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                    inbox = listOf(inbox(OWNER_ID)),
                    workspaceBacklogEntries = emptyList(),
                    workspaceInboxRecords = emptyList(),
                ),
            )

        assertTrue(requireNotNull(result.workspaceBacklogEntries).isEmpty())
        assertTrue(requireNotNull(result.workspaceInboxRecords).isEmpty())
    }

    @Test
    fun `legacy inbox sorting becomes typed canonical capability configuration`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    version = 2,
                    exportedAt = 100L,
                    contexts = listOf(context(OWNER_ID)),
                    contextInboxSortingRules =
                        listOf(
                            ContextInboxSortingSnapshot(
                                contextId = OWNER_ID,
                                rulesText = "attachments:type\nbacklog:oldest",
                                updatedAt = 55L,
                            ),
                        ),
                ),
            )

        val capability =
            requireNotNull(result.workspaceCapabilityInstances)
                .single { it.capabilityType == WorkspaceCapabilityType.INBOX_SORTING.name }
        val configuration =
            InboxSortingCapabilityConfigurationCodec.decode(
                capability.configurationVersion,
                capability.configuration,
            )

        assertEquals(2, configuration.rules.size)
        assertEquals(WorkspaceSortingTarget.CONNECTIONS, configuration.rules[0].target)
        assertEquals(WorkspaceSortingMode.TYPE, configuration.rules[0].mode)
        assertEquals(WorkspaceSortingTarget.BACKLOG, configuration.rules[1].target)
        assertEquals(WorkspaceSortingMode.OLDEST, configuration.rules[1].mode)
        assertTrue(capability.updatedAt >= 55L)
    }

    @Test
    fun `historical non reserved sys id remains ordinary while exact system shell is removed`() {
        val exactSystemId = SystemContexts.INBOX.raw
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context("sys_custom"), context(exactSystemId)),
                ),
            )

        assertNotNull(result.workspaces?.single { it.id == "sys_custom" })
        assertTrue(result.contexts.single { it.id == "sys_custom" }.isDeleted)
        assertTrue(result.contexts.none { it.id == exactSystemId })
        assertNotNull(result.workspaces?.single { it.id == exactSystemId })
    }

    private fun context(
        id: String,
        isDeleted: Boolean = false,
    ) =
        ContextSnapshot(
            id = id,
            name = id,
            parentId = null,
            description = "description",
            createdAt = 10L,
            updatedAt = 20L,
            isExpanded = false,
            isDeleted = isDeleted,
            version = 2L,
            tags = listOf("tag"),
            relatedLinks = emptyList(),
            order = 0,
            isAttachmentsExpanded = false,
            defaultViewModeName = "DIRECTION",
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = 0L,
            valueImportance = 0,
            valueImpact = 0,
            effort = 0,
            cost = 0,
            risk = 0,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0.0,
            displayScore = 0.0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null,
        )

    private fun note(id: String, ownerId: String) =
        LegacyNoteSnapshot(
            id = id,
            contextId = ownerId,
            title = "Note",
            content = "Body",
            createdAt = 11L,
            updatedAt = 21L,
            isDeleted = false,
            version = 3L,
        )

    private fun backlogItem(ownerId: String, targetId: String) =
        BacklogItemSnapshot(
            id = BACKLOG_ID,
            contextId = ownerId,
            itemType = "NOTE",
            entityId = targetId,
            order = 7L,
            updatedAt = 30L,
            version = 4L,
            isDeleted = false,
        )

    private fun inbox(ownerId: String) =
        InboxRecordSnapshot(
            id = INBOX_ID,
            contextId = ownerId,
            text = "Inbox",
            createdAt = 12L,
            order = -12L,
            updatedAt = 31L,
            hideInOwnerInbox = false,
            version = 5L,
            isDeleted = false,
        )

    private companion object {
        const val OWNER_ID = "ordinary-owner"
        const val NOTE_ID = "note-id"
        const val BACKLOG_ID = "backlog-id"
        const val INBOX_ID = "inbox-id"
    }
}
