package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BacklogPresentationLifecycleTest {
    private val canonical = mockk<CanonicalBacklogRepository>()
    private val commands = mockk<BacklogPlacementCommands>(relaxed = true)
    private val attachments = mockk<AttachmentsRepository>(relaxed = true)
    private val lifecycle = BacklogPresentationLifecycle(canonical, commands, attachments)

    @Test
    fun `remove tombstones explicit placement unlinks connection and ignores projection`() = runTest {
        coEvery { canonical.getEntriesByIds(listOf("placement", "attachment", "projection")) } returns
            listOf(entry("placement", isDeleted = false))
        coEvery { attachments.getAttachmentById("attachment") } returns attachment("attachment")
        coEvery { attachments.getAttachmentById("projection") } returns null

        val result = lifecycle.remove("owner", listOf("placement", "attachment", "projection"))

        assertEquals(BacklogPresentationMutationResult(1, 1, 1), result)
        coVerify(exactly = 1) { commands.tombstoneContextBacked(listOf("placement")) }
        coVerify(exactly = 1) { attachments.unlinkAttachmentFromContext("attachment", "owner") }
        coVerify(exactly = 0) { attachments.deleteAttachment(any()) }
    }

    @Test
    fun `undo restores each presentation through its owning capability`() = runTest {
        val placementItem = item("placement")
        val attachmentItem = item("attachment", itemType = "NOTE_DOCUMENT", entityId = "document")
        val projectionItem =
            item(
                "projection",
                associationOwnerContextId = "hashtag-owner",
                associationTag = "tag",
            )
        coEvery { canonical.getEntriesByIds(listOf("placement", "attachment", "projection")) } returns
            listOf(entry("placement", isDeleted = true))
        coEvery { attachments.getAttachmentById("attachment") } returns attachment("attachment")
        coEvery { attachments.getAttachmentById("projection") } returns null

        val result = lifecycle.restore(listOf(placementItem, attachmentItem, projectionItem))

        assertEquals(BacklogPresentationMutationResult(1, 1, 1), result)
        coVerify(exactly = 1) { commands.restoreContextBacked(listOf(placementItem)) }
        coVerify(exactly = 1) { attachments.linkAttachmentToContext("attachment", "owner") }
    }

    private fun entry(
        id: String,
        isDeleted: Boolean,
    ) =
        WorkspaceBacklogEntryEntity(
            id = id,
            workspaceId = "owner",
            capabilityInstanceId = "backlog-owner",
            targetKind = WorkspaceBacklogTargetKind.ORIENTATION.name,
            targetId = "orientation",
            entryOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 1L,
        )

    private fun attachment(id: String) =
        AttachmentEntity(
            id = id,
            attachmentType = "NOTE_DOCUMENT",
            entityId = "document",
            ownerContextId = "owner",
        )

    private fun item(
        id: String,
        itemType: String = "GOAL",
        entityId: String = "goal",
        associationOwnerContextId: String? = null,
        associationTag: String? = null,
    ) =
        BacklogItem(
            id = id,
            contextId = "owner",
            itemType = itemType,
            entityId = entityId,
            associationOwnerContextId = associationOwnerContextId,
            associationTag = associationTag,
            order = 0L,
        )
}
