package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayFocusesRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayFocusesUiState(
    val items: List<DayFocusItem> = emptyList(),
    val availableContexts: List<ProjectOption> = emptyList(),
    val availableAttachments: List<AttachmentOption> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val dialogMode: DayFocusDialogMode? = null,
    val pendingDeleteItem: DayFocusItem? = null,
)

sealed class DayFocusDialogMode {
    data class Create(val type: DayFocusType) : DayFocusDialogMode()

    data class Edit(val item: DayFocusItem) : DayFocusDialogMode()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DayFocusesViewModel
    @Inject
    constructor(
        private val repository: DayFocusesRepository,
        private val contextRepository: ContextRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
    ) : ViewModel() {
        private val planIdFlow = MutableStateFlow<String?>(null)
        private val dialogModeFlow = MutableStateFlow<DayFocusDialogMode?>(null)
        private val pendingDeleteItemFlow = MutableStateFlow<DayFocusItem?>(null)

        private val allContextsFlow =
            contextRepository.getAllContextsFlow()
                .map { contexts ->
                    contexts.map { context ->
                        ProjectOption(
                            id = context.id,
                            name = context.name,
                            parentId = context.parentId,
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        private val allAttachmentsFlow =
            attachmentsRepository.getAttachmentLibraryItems()
                .map { attachments ->
                    attachments.mapNotNull { it.toAttachmentOption() }.filterNot { it.linkType == LinkType.CONTEXT }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        val uiState: StateFlow<DayFocusesUiState> =
            combine(
                planIdFlow.flatMapLatest { planId ->
                    if (planId == null) {
                        flowOf(DayFocusesUiState(isLoading = true))
                    } else {
                        repository
                            .getItemsForDayPlan(planId)
                            .map { items ->
                                DayFocusesUiState(
                                    items = items,
                                    isLoading = false,
                                )
                            }.catch { throwable ->
                                emit(
                                    DayFocusesUiState(
                                        isLoading = false,
                                        error = throwable.message ?: "Не вдалося завантажити фокуси дня",
                                    ),
                                )
                            }
                    }
                },
                allContextsFlow,
                allAttachmentsFlow,
                dialogModeFlow,
                pendingDeleteItemFlow,
            ) { baseState, availableContexts, availableAttachments, dialogMode, pendingDeleteItem ->
                baseState.copy(
                    availableContexts = availableContexts,
                    availableAttachments = availableAttachments,
                    dialogMode = dialogMode,
                    pendingDeleteItem = pendingDeleteItem,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DayFocusesUiState(),
            )

        fun loadDataForPlan(planId: String) {
            if (planIdFlow.value == planId) return
            planIdFlow.value = planId
        }

        fun openCreateDialog(type: DayFocusType) {
            dialogModeFlow.value = DayFocusDialogMode.Create(type)
        }

        fun openEditDialog(item: DayFocusItem) {
            dialogModeFlow.value = DayFocusDialogMode.Edit(item)
        }

        fun dismissDialog() {
            dialogModeFlow.value = null
        }

        fun requestDelete(item: DayFocusItem) {
            pendingDeleteItemFlow.value = item
        }

        fun dismissDeleteRequest() {
            pendingDeleteItemFlow.value = null
        }

        fun saveItem(
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            isEveryday: Boolean,
            budgetPercent: Int?,
        ) {
            val trimmedTitle = title.trim()
            val planId = planIdFlow.value ?: return
            if (trimmedTitle.isBlank()) return

            viewModelScope.launch {
                when (val dialogMode = dialogModeFlow.value) {
	                    is DayFocusDialogMode.Edit ->
	                        repository.updateItem(
	                            item = dialogMode.item,
	                            title = trimmedTitle,
	                            notes = notes,
	                            relatedLinks = relatedLinks,
	                            type = type,
	                            isEveryday = isEveryday,
	                            budgetPercent = budgetPercent,
	                        )

                    else ->
                        repository.addItem(
	                            dayPlanId = planId,
	                            title = trimmedTitle,
	                            notes = notes,
	                            relatedLinks = relatedLinks,
	                            type = type,
	                            order = uiState.value.items.size.toLong(),
	                            isEveryday = isEveryday,
	                            budgetPercent = budgetPercent,
	                        )
                }
                dialogModeFlow.value = null
            }
        }

        fun addQuickFocus(
            title: String,
            type: DayFocusType = DayFocusType.FOCUS,
        ) {
            val trimmedTitle = title.trim()
            val planId = planIdFlow.value ?: return
            if (trimmedTitle.isBlank()) return

            viewModelScope.launch {
                repository.addItem(
                    dayPlanId = planId,
                    title = trimmedTitle,
                    notes = null,
                    relatedLinks = emptyList(),
                    type = type,
                    order = uiState.value.items.size.toLong(),
                    isEveryday = false,
                    budgetPercent = null,
                )
            }
        }

        fun confirmDeleteCurrentOnly() {
            val item = pendingDeleteItemFlow.value ?: return
            viewModelScope.launch {
                repository.deleteItem(item.id)
                pendingDeleteItemFlow.value = null
            }
        }

        fun confirmDeleteEverywhere() {
            val item = pendingDeleteItemFlow.value ?: return
            val recurringKey = item.recurringKey ?: item.id
            viewModelScope.launch {
                repository.deleteItemEverywhere(recurringKey)
                pendingDeleteItemFlow.value = null
            }
        }

        fun updateItemsOrder(reorderedItems: List<DayFocusItem>) {
            viewModelScope.launch {
                repository.reorderItems(reorderedItems)
            }
        }

        suspend fun createDocumentForPicker(request: NewDocumentDraft): String? =
            when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "Нова нотатка" },
                            contextId = SystemContexts.TODAY.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.JournalDocument -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "Новий журнал" },
                            contextId = SystemContexts.TODAY.raw,
                            attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId =
                        musicNoteRepository.create(
                            name = request.name.ifBlank { "Нові ноти" },
                            contextId = SystemContexts.TODAY.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)?.id
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId =
                        checklistRepository.createChecklist(
                            name = request.name.ifBlank { "Новий чекліст" },
                            contextId = SystemContexts.TODAY.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)?.id
                }
                is NewDocumentDraft.WebLink -> {
                    val target = request.url.trim()
                    target.takeIf { it.isNotBlank() }?.let { nonBlankTarget ->
                        attachmentsRepository.createLinkAttachment(
                            contextId = SystemContexts.TODAY.raw,
                            link =
                                RelatedLink(
                                    type = LinkType.URL,
                                    target = nonBlankTarget,
                                    displayName = request.name.trim().ifBlank { nonBlankTarget },
                                ),
                        )
                    }
                }
                is NewDocumentDraft.Obsidian -> {
                    val target = request.noteName.trim()
                    target.takeIf { it.isNotBlank() }?.let { nonBlankTarget ->
                        attachmentsRepository.createLinkAttachment(
                            contextId = SystemContexts.TODAY.raw,
                            link =
                                RelatedLink(
                                    type = LinkType.OBSIDIAN,
                                    target = nonBlankTarget,
                                    displayName = request.displayName.trim().ifBlank { nonBlankTarget },
                                    vault = request.vault,
                                ),
                        )
                    }
                }
            }
    }

private fun AttachmentLibraryQueryResult.toAttachmentOption(): AttachmentOption {
    val relatedLink =
        linkDisplayName?.let { json ->
            runCatching { Gson().fromJson(json, RelatedLink::class.java) }.getOrNull()
        }
    val label =
        noteName?.takeIf { it.isNotBlank() }
            ?: musicNoteName?.takeIf { it.isNotBlank() }
            ?: checklistName?.takeIf { it.isNotBlank() }
            ?: scriptName?.takeIf { it.isNotBlank() }
            ?: relatedLink?.displayName?.takeIf { it.isNotBlank() }
            ?: relatedLink?.target?.takeIf { it.isNotBlank() }
            ?: contextName
            ?: "Attachment ${id.takeLast(4)}"

    return AttachmentOption(
        id = id,
        name = label,
        linkType = relatedLink?.type,
        attachmentType = attachmentType,
        entityId = entityId,
        target = relatedLink?.target,
        vault = relatedLink?.vault,
    )
}
