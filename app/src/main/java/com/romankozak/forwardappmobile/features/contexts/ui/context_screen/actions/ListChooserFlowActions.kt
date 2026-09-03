package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository

class ListChooserFlowActions(
    private val contextRepository: ContextRepository,
    private val directionRepository: DirectionRepository,
) {
    data class DirectionAddResult(
        val errorMessage: String? = null,
    )

    data class AttachmentShareResult(
        val message: String,
        val newlyAddedItemId: String? = null,
        val shouldRefreshCurrentContext: Boolean = false,
    )

    suspend fun addDirectionLinkedToContext(
        targetContextId: String,
        currentContextId: String,
    ): DirectionAddResult {
        val linkedContextId = targetContextId.takeIf { it.isNotBlank() && it != "root" }
        if (linkedContextId == null) {
            return DirectionAddResult(errorMessage = "Оберіть контекст для напрямку.")
        }
        val linkedContextName =
            contextRepository.getContextById(linkedContextId)?.name?.takeIf { it.isNotBlank() }
                ?: "Context"
        directionRepository.addDirectionItem(
            contextId = currentContextId,
            text = linkedContextName,
            linkedContextId = linkedContextId,
        )
        return DirectionAddResult()
    }

    suspend fun shareAttachmentToProject(
        attachment: BacklogItemContent,
        targetContextId: String,
        currentContextId: String,
    ): AttachmentShareResult {
        val isAttachmentSupported =
            attachment is BacklogItemContent.LinkItem ||
                attachment is BacklogItemContent.NoteDocumentItem ||
                attachment is BacklogItemContent.MusicNoteItem ||
                attachment is BacklogItemContent.ChecklistItem
        val itemType = attachment.backlogItem.itemType
        val entityId = attachment.backlogItem.entityId
        return when {
            !isAttachmentSupported ->
                AttachmentShareResult(message = "This attachment type does not support copying")
            itemType == null || entityId == null ->
                AttachmentShareResult(message = "Cannot share corrupt attachment")
            else -> {
                val attachmentId =
                    runCatching {
                        contextRepository.ensureAttachmentLinkedToContext(
                            attachmentType = itemType,
                            entityId = entityId,
                            targetContextId = targetContextId,
                            ownerContextId =
                                attachment.backlogItem.contextId.takeIf { it.isNotBlank() } ?: currentContextId,
                        )
                        attachment.backlogItem.entityId
                    }.onFailure { error ->
                        Log.e("ListChooserFlowActions", "Failed to link attachment", error)
                    }.getOrNull()

                AttachmentShareResult(
                    message = "Attachment added to selected context",
                    newlyAddedItemId = attachmentId,
                    shouldRefreshCurrentContext = targetContextId == currentContextId && attachmentId != null,
                )
            }
        }
    }
}
