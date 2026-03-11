package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextKeyProblemsRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import java.util.UUID

class ContextPickerActions(
    private val contextRepository: ContextRepository,
    private val contextKeyProblemsRepository: ContextKeyProblemsRepository,
    private val focusContextRepository: FocusContextRepository,
    private val listChooserFlowActions: ListChooserFlowActions,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val musicNoteRepository: MusicNoteRepository,
    private val checklistRepository: ChecklistRepository,
    private val loggerTag: String,
) {
    suspend fun onPickerContextSelected(
        currentContextId: String,
        targetContextId: String,
        showSnackbar: (String) -> Unit,
    ) {
        if (targetContextId.isBlank() || currentContextId.isBlank()) return
        if (targetContextId == currentContextId) {
            showSnackbar("Цей контекст вже відкритий")
            return
        }

        val targetName = contextRepository.getContextById(targetContextId)?.name?.ifBlank { null } ?: targetContextId
        contextRepository.addLinkItemToContextFromLink(
            contextId = currentContextId,
            link =
                RelatedLink(
                    type = LinkType.CONTEXT,
                    target = targetContextId,
                    displayName = targetName,
                ),
        )
    }

    suspend fun onBacklogContextLinkSelected(
        currentContextId: String,
        targetContextId: String,
        showSnackbar: (String) -> Unit,
        forceRefresh: () -> Unit,
    ) {
        if (targetContextId.isBlank() || currentContextId.isBlank()) return
        if (targetContextId == currentContextId) {
            showSnackbar("Цей контекст вже відкритий")
            return
        }
        if (contextRepository.doesLinkToContextExist(targetContextId, currentContextId)) {
            showSnackbar("Посилання на цей контекст вже є в беклозі")
            return
        }

        contextRepository.addContextLinkToContext(targetContextId, currentContextId)
        forceRefresh()
    }

    suspend fun onDirectionContextLinkSelected(
        currentContextId: String,
        targetContextId: String,
        directionItems: List<DirectionItemEntity>,
        showSnackbar: (String) -> Unit,
    ) {
        if (targetContextId.isBlank() || currentContextId.isBlank()) return
        if (targetContextId == currentContextId) {
            showSnackbar("Цей контекст вже відкритий")
            return
        }
        if (directionItems.any { it.linkedContextId == targetContextId }) {
            showSnackbar("Посилання на цей контекст вже є в напрямку")
            return
        }

        val result =
            listChooserFlowActions.addDirectionLinkedToContext(
                targetContextId = targetContextId,
                currentContextId = currentContextId,
            )
        result.errorMessage?.let(showSnackbar)
    }

    suspend fun onKeyProblemsDescriptionChanged(
        currentContextId: String,
        description: String,
    ) {
        if (currentContextId.isBlank()) return
        contextKeyProblemsRepository.updateDescription(
            contextId = currentContextId,
            description = description,
        )
    }

    suspend fun addKeyProblemsFocusContext(
        currentContextId: String,
        targetContextId: String,
    ) {
        if (currentContextId.isBlank() || targetContextId.isBlank()) return
        contextKeyProblemsRepository.addFocusContext(currentContextId, targetContextId)
    }

    suspend fun removeKeyProblemsFocusContext(
        currentContextId: String,
        targetContextId: String,
    ) {
        if (currentContextId.isBlank() || targetContextId.isBlank()) return
        contextKeyProblemsRepository.removeFocusContext(currentContextId, targetContextId)
    }

    suspend fun toggleCurrentContextFocus(
        contextId: String,
        showSnackbar: (String) -> Unit,
    ) {
        if (contextId.isBlank()) return
        val focused = focusContextRepository.toggleFocusContext(contextId)
        showSnackbar(
            if (focused) {
                "Контекст додано у фокус"
            } else {
                "Контекст прибрано з фокусу"
            },
        )
    }

    suspend fun onPickerAttachmentSelected(
        currentContextId: String,
        attachmentId: String,
    ) {
        if (attachmentId.isBlank()) return
        if (currentContextId.isBlank()) return
        runCatching {
            contextRepository.linkAttachmentToContext(attachmentId, currentContextId)
        }.onFailure {
            Log.e(loggerTag, "Failed to link attachment to current context", it)
        }
    }

    suspend fun createRootContextForPicker(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null
        val id = UUID.randomUUID().toString()
        contextRepository.createContextWithId(
            id = id,
            name = trimmed,
            parentId = null,
        )
        return id
    }

    suspend fun createAttachmentForPicker(
        currentContextId: String,
        request: NewDocumentDraft,
    ): String? {
        if (currentContextId.isBlank()) return null

        return when (request) {
            is NewDocumentDraft.Note -> {
                val documentId =
                    noteDocumentRepository.createDocument(
                        name = request.name.ifBlank { "New note" },
                        contextId = currentContextId,
                    )
                contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)
            }
            is NewDocumentDraft.MusicNote -> {
                val musicNoteId =
                    musicNoteRepository.create(
                        name = request.name.ifBlank { "New music note" },
                        contextId = currentContextId,
                    )
                contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)
            }
            is NewDocumentDraft.Checklist -> {
                val checklistId =
                    checklistRepository.createChecklist(
                        name = request.name.ifBlank { "New checklist" },
                        contextId = currentContextId,
                    )
                contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)
            }
            is NewDocumentDraft.WebLink -> {
                val target = request.url.trim()
                if (target.isBlank()) return null
                contextRepository.addLinkItemToContextFromLink(
                    contextId = currentContextId,
                    link =
                        RelatedLink(
                            type = LinkType.URL,
                            target = target,
                            displayName = request.name.trim().ifBlank { target },
                        ),
                )
            }
            is NewDocumentDraft.Obsidian -> {
                val target = request.noteName.trim()
                if (target.isBlank()) return null
                contextRepository.addLinkItemToContextFromLink(
                    contextId = currentContextId,
                    link =
                        RelatedLink(
                            type = LinkType.OBSIDIAN,
                            target = target,
                            displayName = request.displayName.trim().ifBlank { target },
                        ),
                )
            }
        }
    }
}
