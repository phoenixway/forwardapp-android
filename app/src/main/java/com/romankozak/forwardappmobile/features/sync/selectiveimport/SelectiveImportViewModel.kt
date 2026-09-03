package com.romankozak.forwardappmobile.features.sync.selectiveimport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.shared.application.imports.WorkspaceImportSessionEffect
import com.romankozak.forwardappmobile.shared.application.imports.WorkspaceImportSessionIntent
import com.romankozak.forwardappmobile.shared.application.imports.WorkspaceImportSessionStore
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewItemStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewModel
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSection
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSectionKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSectionSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SelectiveImportViewModel
    @Inject
    constructor(
        private val loadSelectiveImportPreviewUseCase: LoadSelectiveImportPreviewUseCase,
        private val selectiveImportCoordinator: SelectiveImportCoordinator,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SelectiveImportState())
        val uiState = _uiState.asStateFlow()
        private val importSessionStore = WorkspaceImportSessionStore()

        private val _eventChannel = Channel<SelectiveImportEvent>()
        val eventFlow = _eventChannel.receiveAsFlow()

        init {
            viewModelScope.launch {
                importSessionStore.state.collectLatest { sessionState ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = sessionState.isBusy,
                            error = sessionState.errorMessage,
                            sourceMode = sessionState.descriptor?.sourceMode,
                            sourceFormat = sessionState.descriptor?.format,
                            previewModel = sessionState.previewModel,
                            previewSummary = sessionState.previewSummary,
                            selection = sessionState.selection,
                        )
                    }

                    if (sessionState.pendingEffect == WorkspaceImportSessionEffect.NavigateBack) {
                        _eventChannel.send(SelectiveImportEvent.NavigateBack)
                        importSessionStore.dispatch(WorkspaceImportSessionIntent.EffectConsumed)
                    }
                }
            }
            val uri = savedStateHandle.get<String>("fileUri")
            Timber.tag("IMPORT_SELECTIVE_INIT").d("Init called, fileUri from SavedStateHandle: $uri")
            loadBackupFile(uri)
        }

        internal fun loadBackupFile(fileUriString: String?) {
            viewModelScope.launch {
                Timber.tag("IMPORT_SELECTIVE").d("loadBackupFile called with: $fileUriString")
                if (fileUriString == null) {
                    Timber.tag("IMPORT_SELECTIVE").d("File URI is null!")
                    importSessionStore.dispatch(
                        WorkspaceImportSessionIntent.PreviewFailed("File URI not provided."),
                    )
                    return@launch
                }

                importSessionStore.dispatch(WorkspaceImportSessionIntent.PreviewLoadingStarted)

                loadSelectiveImportPreviewUseCase(fileUriString)
                    .onSuccess { preview ->
                        importSessionStore.dispatch(
                            WorkspaceImportSessionIntent.PreviewLoaded(
                                descriptor =
                                    com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportDescriptor(
                                        format = preview.sourceFormat,
                                        sourceMode = preview.sourceMode,
                                    ),
                            ),
                        )
                        _uiState.update {
                            it.copy(
                                backupContent = preview.backupContent,
                                sourceSnapshotBundle = preview.sourceSnapshotBundle,
                            )
                        }
                        syncPreviewModel(preview.backupContent)
                        syncPreviewSummary(preview.backupContent)
                        syncSelection(preview.backupContent)
                    }
                    .onFailure { error ->
                        importSessionStore.dispatch(
                            WorkspaceImportSessionIntent.PreviewFailed(
                                error.message ?: "Failed to parse backup file.",
                            ),
                        )
                    }
            }
        }

        fun onImportClicked() {
            viewModelScope.launch {
                importSessionStore.dispatch(WorkspaceImportSessionIntent.ImportStarted)
                val result = selectiveImportCoordinator.importSelection(_uiState.value)

                if (result.isSuccess) {
                    importSessionStore.dispatch(WorkspaceImportSessionIntent.ImportSucceeded)
                } else {
                    importSessionStore.dispatch(
                        WorkspaceImportSessionIntent.ImportFailed(
                            result.exceptionOrNull()?.message ?: "Unknown import error",
                        ),
                    )
                }
            }
        }

        fun toggleProjectSelection(
            contextId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedProjects =
                    currentState.backupContent?.projects?.map {
                        if (it.item.id == contextId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(projects = updatedProjects ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Contexts, contextId, isSelected)
        }

        fun toggleGoalSelection(
            goalId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedGoals =
                    currentState.backupContent?.goals?.map {
                        if (it.item.id == goalId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(goals = updatedGoals ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Goals, goalId, isSelected)
        }

        fun toggleCanonicalBacklogSelection(
            placementId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedEntries =
                    currentState.backupContent?.workspaceBacklogEntries?.map {
                        if (it.item.entry.id == placementId && it.isSelectable) {
                            it.copy(isSelected = isSelected)
                        } else {
                            it
                        }
                    }
                currentState.copy(
                    backupContent =
                        currentState.backupContent?.copy(
                            workspaceBacklogEntries = updatedEntries.orEmpty(),
                        ),
                )
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Backlog, placementId, isSelected)
        }

        fun toggleLegacyNoteSelection(
            noteId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedNotes =
                    currentState.backupContent?.legacyNotes?.map {
                        if (it.item.id == noteId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(legacyNotes = updatedNotes ?: emptyList()))
            }
            syncPreviewModel()
            syncSelection()
            syncPreviewSummary()
        }

        fun toggleActivityRecordSelection(
            recordId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedRecords =
                    currentState.backupContent?.activityRecords?.map {
                        if (it.item.id == recordId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(activityRecords = updatedRecords ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.ActivityRecords, recordId, isSelected)
        }

        fun toggleDocumentSelection(
            documentId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedDocuments =
                    currentState.backupContent?.documents?.map {
                        if (it.item.id == documentId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(documents = updatedDocuments ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Documents, documentId, isSelected)
        }

        fun toggleChecklistSelection(
            checklistId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedChecklists =
                    currentState.backupContent?.checklists?.map {
                        if (it.item.id == checklistId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(checklists = updatedChecklists ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Checklists, checklistId, isSelected)
        }

        fun toggleLinkItemSelection(
            linkId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedLinks =
                    currentState.backupContent?.linkItems?.map {
                        if (it.item.id == linkId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(linkItems = updatedLinks ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.LinkItems, linkId, isSelected)
        }

        fun toggleInboxRecordSelection(
            recordId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedRecords =
                    currentState.backupContent?.inboxRecords?.map {
                        if (it.item.id == recordId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(inboxRecords = updatedRecords ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.InboxRecords, recordId, isSelected)
        }

        fun toggleProjectExecutionLogSelection(
            logId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedLogs =
                    currentState.backupContent?.contextLogs?.map {
                        if (it.item.id == logId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(contextLogs = updatedLogs ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.ContextLogs, logId, isSelected)
        }

        fun toggleScriptSelection(
            scriptId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedScripts =
                    currentState.backupContent?.scripts?.map {
                        if (it.item.id == scriptId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(scripts = updatedScripts ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Scripts, scriptId, isSelected)
        }

        fun toggleAttachmentSelection(
            attachmentId: String,
            isSelected: Boolean,
        ) {
            _uiState.update { currentState ->
                val updatedAttachments =
                    currentState.backupContent?.attachments?.map {
                        if (it.item.id == attachmentId && it.isSelectable) it.copy(isSelected = isSelected) else it
                    }
                currentState.copy(backupContent = currentState.backupContent?.copy(attachments = updatedAttachments ?: emptyList()))
            }
            onItemSelectionChanged(WorkspaceImportPreviewSectionKind.Attachments, attachmentId, isSelected)
        }

        fun toggleAllSelection(
            entityType: EntityType,
            selectAll: Boolean,
        ) {
            _uiState.update { currentState ->
                val content = currentState.backupContent ?: return@update currentState
                val updatedContent =
                    when (entityType) {
                        EntityType.PROJECT ->
                            content.copy(
                                projects = content.projects.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it },
                            )
                        EntityType.GOAL ->
                            content.copy(
                                goals = content.goals.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it },
                            )
                        EntityType.BACKLOG ->
                            content.copy(
                                workspaceBacklogEntries =
                                    content.workspaceBacklogEntries.map {
                                        if (it.isSelectable) it.copy(isSelected = selectAll) else it
                                    },
                            )
                        EntityType.LEGACY_NOTE ->
                            content.copy(
                                legacyNotes =
                                    content.legacyNotes.map {
                                        if (it.isSelectable) it.copy(isSelected = selectAll) else it
                                    },
                            )
                        EntityType.ACTIVITY_RECORD ->
                            content.copy(
                                activityRecords =
                                    content.activityRecords.map {
                                        if (it.isSelectable) it.copy(isSelected = selectAll) else it
                                    },
                            )
                        EntityType.DOCUMENT ->
                            content.copy(
                                documents = content.documents.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it },
                            )
                        EntityType.CHECKLIST ->
                            content.copy(
                                checklists = content.checklists.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it },
                            )
                        EntityType.LINK_ITEM ->
                            content.copy(
                                linkItems = content.linkItems.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it },
                            )
                        EntityType.INBOX_RECORD ->
                            content.copy(
                                inboxRecords =
                                    content.inboxRecords.map {
                                        if (it.isSelectable) it.copy(isSelected = selectAll) else it
                                    },
                            )
                        EntityType.PROJECT_EXECUTION_LOG ->
                            content.copy(
                                contextLogs =
                                    content.contextLogs.map {
                                        if (it.isSelectable) it.copy(isSelected = selectAll) else it
                                    },
                            )
                        EntityType.SCRIPT ->
                            content.copy(
                                scripts = content.scripts.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it },
                            )
                        EntityType.ATTACHMENT ->
                            content.copy(
                                attachments =
                                    content.attachments.map {
                                        if (it.isSelectable) it.copy(isSelected = selectAll) else it
                                    },
                            )
                    }
                currentState.copy(backupContent = updatedContent)
            }
            syncPreviewModel()
            onSectionSelectionChanged(entityType, selectAll)
        }

        private fun syncSelection(content: SelectableDatabaseContent? = _uiState.value.backupContent) {
            importSessionStore.dispatch(
                WorkspaceImportSessionIntent.SelectionReplaced(
                    selection = content.toWorkspaceSelectiveImportSelection(),
                ),
            )
        }

        private fun syncPreviewSummary(content: SelectableDatabaseContent? = _uiState.value.backupContent) {
            importSessionStore.dispatch(
                WorkspaceImportSessionIntent.PreviewSummaryReplaced(
                    summary = content.toWorkspaceImportPreviewSummary(),
                ),
            )
        }

        private fun syncPreviewModel(content: SelectableDatabaseContent? = _uiState.value.backupContent) {
            importSessionStore.dispatch(
                WorkspaceImportSessionIntent.PreviewModelReplaced(
                    model = content.toWorkspaceImportPreviewModel(),
                ),
            )
        }

        private fun onItemSelectionChanged(
            kind: WorkspaceImportPreviewSectionKind,
            itemId: String,
            isSelected: Boolean,
        ) {
            importSessionStore.dispatch(
                WorkspaceImportSessionIntent.ItemSelectionChanged(
                    kind = kind,
                    itemId = itemId,
                    isSelected = isSelected,
                ),
            )
        }

        private fun onSectionSelectionChanged(
            entityType: EntityType,
            isSelected: Boolean,
        ) {
            val content = _uiState.value.backupContent ?: return
            val (kind, itemIds) =
                when (entityType) {
                    EntityType.PROJECT -> WorkspaceImportPreviewSectionKind.Contexts to content.projects.selectedCandidateIds()
                    EntityType.GOAL -> WorkspaceImportPreviewSectionKind.Goals to content.goals.selectedCandidateIds()
                    EntityType.BACKLOG ->
                        WorkspaceImportPreviewSectionKind.Backlog to
                            content.workspaceBacklogEntries.selectedCandidateIds()
                    EntityType.LEGACY_NOTE -> {
                        syncSelection()
                        syncPreviewSummary()
                        return
                    }
                    EntityType.ACTIVITY_RECORD ->
                        WorkspaceImportPreviewSectionKind.ActivityRecords to content.activityRecords.selectedCandidateIds()
                    EntityType.DOCUMENT ->
                        WorkspaceImportPreviewSectionKind.Documents to content.documents.selectedCandidateIds()
                    EntityType.CHECKLIST ->
                        WorkspaceImportPreviewSectionKind.Checklists to content.checklists.selectedCandidateIds()
                    EntityType.LINK_ITEM ->
                        WorkspaceImportPreviewSectionKind.LinkItems to content.linkItems.selectedCandidateIds()
                    EntityType.INBOX_RECORD ->
                        WorkspaceImportPreviewSectionKind.InboxRecords to content.inboxRecords.selectedCandidateIds()
                    EntityType.PROJECT_EXECUTION_LOG ->
                        WorkspaceImportPreviewSectionKind.ContextLogs to content.contextLogs.selectedCandidateIds()
                    EntityType.SCRIPT ->
                        WorkspaceImportPreviewSectionKind.Scripts to content.scripts.selectedCandidateIds()
                    EntityType.ATTACHMENT ->
                        WorkspaceImportPreviewSectionKind.Attachments to content.attachments.selectedCandidateIds()
                }

            importSessionStore.dispatch(
                WorkspaceImportSessionIntent.SectionSelectionChanged(
                    kind = kind,
                    itemIds = itemIds,
                    isSelected = isSelected,
                ),
            )
        }
    }

enum class EntityType {
    PROJECT,
    GOAL,
    BACKLOG,
    LEGACY_NOTE,
    ACTIVITY_RECORD,
    DOCUMENT,
    CHECKLIST,
    LINK_ITEM,
    INBOX_RECORD,
    PROJECT_EXECUTION_LOG,
    SCRIPT,
    ATTACHMENT,
}

sealed interface SelectiveImportEvent {
    object NavigateBack : SelectiveImportEvent
}

fun SelectiveImportViewModel.onPreviewItemToggle(
    kind: WorkspaceImportPreviewSectionKind,
    itemId: String,
    isSelected: Boolean,
) {
    when (kind) {
        WorkspaceImportPreviewSectionKind.Contexts -> toggleProjectSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.Goals -> toggleGoalSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.Backlog -> toggleCanonicalBacklogSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.LegacyNotes -> toggleLegacyNoteSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.ActivityRecords -> toggleActivityRecordSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.Documents -> toggleDocumentSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.Checklists -> toggleChecklistSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.LinkItems -> toggleLinkItemSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.InboxRecords -> toggleInboxRecordSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.ContextLogs -> toggleProjectExecutionLogSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.Scripts -> toggleScriptSelection(itemId, isSelected)
        WorkspaceImportPreviewSectionKind.Attachments -> toggleAttachmentSelection(itemId, isSelected)
    }
}

fun SelectiveImportViewModel.onPreviewSectionToggle(
    kind: WorkspaceImportPreviewSectionKind,
    selectAll: Boolean,
) {
    when (kind) {
        WorkspaceImportPreviewSectionKind.Contexts -> toggleAllSelection(EntityType.PROJECT, selectAll)
        WorkspaceImportPreviewSectionKind.Goals -> toggleAllSelection(EntityType.GOAL, selectAll)
        WorkspaceImportPreviewSectionKind.Backlog -> toggleAllSelection(EntityType.BACKLOG, selectAll)
        WorkspaceImportPreviewSectionKind.LegacyNotes -> toggleAllSelection(EntityType.LEGACY_NOTE, selectAll)
        WorkspaceImportPreviewSectionKind.ActivityRecords -> toggleAllSelection(EntityType.ACTIVITY_RECORD, selectAll)
        WorkspaceImportPreviewSectionKind.Documents -> toggleAllSelection(EntityType.DOCUMENT, selectAll)
        WorkspaceImportPreviewSectionKind.Checklists -> toggleAllSelection(EntityType.CHECKLIST, selectAll)
        WorkspaceImportPreviewSectionKind.LinkItems -> toggleAllSelection(EntityType.LINK_ITEM, selectAll)
        WorkspaceImportPreviewSectionKind.InboxRecords -> toggleAllSelection(EntityType.INBOX_RECORD, selectAll)
        WorkspaceImportPreviewSectionKind.ContextLogs -> toggleAllSelection(EntityType.PROJECT_EXECUTION_LOG, selectAll)
        WorkspaceImportPreviewSectionKind.Scripts -> toggleAllSelection(EntityType.SCRIPT, selectAll)
        WorkspaceImportPreviewSectionKind.Attachments -> toggleAllSelection(EntityType.ATTACHMENT, selectAll)
    }
}

internal fun SelectableDatabaseContent?.toWorkspaceSelectiveImportSelection() =
    com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection(
        selectedContextIds = this?.projects.selectedIds().orEmpty(),
        selectedGoalIds = this?.goals.selectedIds().orEmpty(),
        selectedWorkspaceBacklogEntryIds = this?.workspaceBacklogEntries.selectedIds().orEmpty(),
        selectedDocumentIds = this?.documents.selectedIds().orEmpty(),
        selectedChecklistIds = this?.checklists.selectedIds().orEmpty(),
        selectedLinkItemIds = this?.linkItems.selectedIds().orEmpty(),
        selectedInboxRecordIds = this?.inboxRecords.selectedIds().orEmpty(),
        selectedContextLogIds = this?.contextLogs.selectedIds().orEmpty(),
        selectedScriptIds = this?.scripts.selectedIds().orEmpty(),
        selectedAttachmentIds = this?.attachments.selectedIds().orEmpty(),
        selectedActivityRecordIds = this?.activityRecords.selectedIds().orEmpty(),
    )

internal fun SelectableDatabaseContent?.toWorkspaceImportPreviewSummary(): WorkspaceImportPreviewSummary =
    WorkspaceImportPreviewSummary(
        sections =
            listOfNotNull(
                this?.projects?.toSectionSummary(WorkspaceImportPreviewSectionKind.Contexts),
                this?.goals?.toSectionSummary(WorkspaceImportPreviewSectionKind.Goals),
                this?.workspaceBacklogEntries
                    ?.takeIf { it.isNotEmpty() }
                    ?.toSectionSummary(WorkspaceImportPreviewSectionKind.Backlog),
                this?.legacyNotes?.toSectionSummary(WorkspaceImportPreviewSectionKind.LegacyNotes),
                this?.activityRecords?.toSectionSummary(WorkspaceImportPreviewSectionKind.ActivityRecords),
                this?.documents?.toSectionSummary(WorkspaceImportPreviewSectionKind.Documents),
                this?.checklists?.toSectionSummary(WorkspaceImportPreviewSectionKind.Checklists),
                this?.linkItems?.toSectionSummary(WorkspaceImportPreviewSectionKind.LinkItems),
                this?.inboxRecords?.toSectionSummary(WorkspaceImportPreviewSectionKind.InboxRecords),
                this?.contextLogs?.toSectionSummary(WorkspaceImportPreviewSectionKind.ContextLogs),
                this?.scripts?.toSectionSummary(WorkspaceImportPreviewSectionKind.Scripts),
                this?.attachments?.toSectionSummary(WorkspaceImportPreviewSectionKind.Attachments),
            ),
    )

internal fun SelectableDatabaseContent?.toWorkspaceImportPreviewModel(): WorkspaceImportPreviewModel =
    WorkspaceImportPreviewModel(
        sections =
            listOfNotNull(
                this?.projects?.toPreviewSection(WorkspaceImportPreviewSectionKind.Contexts) { it.id to (it.name to null) },
                this?.goals?.toPreviewSection(WorkspaceImportPreviewSectionKind.Goals) { it.id to (it.text to null) },
                this?.workspaceBacklogEntries
                    ?.takeIf { it.isNotEmpty() }
                    ?.toPreviewSection(WorkspaceImportPreviewSectionKind.Backlog) {
                        it.entry.id to (it.title to it.subtitle)
                    },
                this?.legacyNotes?.toPreviewSection(WorkspaceImportPreviewSectionKind.LegacyNotes) {
                    it.id to ((it.title.ifBlank { "Без назви" }) to null)
                },
                this?.activityRecords?.toPreviewSection(WorkspaceImportPreviewSectionKind.ActivityRecords) {
                    it.id to ((it.text.ifBlank { "Без опису" }) to null)
                },
                this?.documents?.toPreviewSection(WorkspaceImportPreviewSectionKind.Documents) {
                    it.id to ((it.name.ifBlank { "Без назви" }) to null)
                },
                this?.checklists?.toPreviewSection(WorkspaceImportPreviewSectionKind.Checklists) {
                    it.id to ((it.name.ifBlank { "Без назви" }) to null)
                },
                this?.linkItems?.toPreviewSection(WorkspaceImportPreviewSectionKind.LinkItems) {
                    it.id to (((it.linkData.displayName ?: it.linkData.target).ifBlank { "Без назви" }) to null)
                },
                this?.inboxRecords?.toPreviewSection(WorkspaceImportPreviewSectionKind.InboxRecords) {
                    it.id to ((it.text.ifBlank { "Без вмісту" }) to null)
                },
                this?.contextLogs?.toPreviewSection(WorkspaceImportPreviewSectionKind.ContextLogs) {
                    it.id to ("Log ${it.id.take(8)}" to null)
                },
                this?.scripts?.toPreviewSection(WorkspaceImportPreviewSectionKind.Scripts) {
                    it.id to ((it.name.ifBlank { "Без назви" }) to null)
                },
                this?.attachments?.toPreviewSection(WorkspaceImportPreviewSectionKind.Attachments) {
                    it.id to ((it.attachmentType.ifBlank { "Без назви" }) to null)
                },
            ),
    )

private fun <T> List<SelectableDiffItem<T>>?.selectedIds(): Set<String> =
    this.orEmpty().asSequence()
        .filter { it.isSelectable && it.isSelected }
        .mapNotNull { item -> extractSelectionId(item.item as Any) }
        .toSet()

private fun <T> List<SelectableDiffItem<T>>.selectedCandidateIds(): Set<String> =
    asSequence()
        .filter { it.isSelectable }
        .mapNotNull { item -> extractSelectionId(item.item as Any) }
        .toSet()

private fun extractSelectionId(item: Any): String? =
    when (item) {
        is com.romankozak.forwardappmobile.core.data.models.entities.Context -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.Goal -> item.id
        is CanonicalBacklogPreviewRow -> item.entry.id
        is com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.ContextLog -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity -> item.id
        is com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord -> item.id
        else -> null
    }

private fun <T> List<SelectableDiffItem<T>>.toSectionSummary(
    kind: WorkspaceImportPreviewSectionKind,
): WorkspaceImportPreviewSectionSummary =
    WorkspaceImportPreviewSectionSummary(
        kind = kind,
        totalCount = size,
        selectedCount = count { it.isSelectable && it.isSelected },
        newCount = count { it.status == com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.NEW },
        updatedCount = count { it.status == com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.UPDATED },
        deletedCount = count { it.status == com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.DELETED },
    )

private fun <T> List<SelectableDiffItem<T>>.toPreviewSection(
    kind: WorkspaceImportPreviewSectionKind,
    mapper: (T) -> Pair<String, Pair<String, String?>>,
): WorkspaceImportPreviewSection =
    WorkspaceImportPreviewSection(
        kind = kind,
        title = kind.title,
        items =
            map { selectable ->
                val (id, text) = mapper(selectable.item)
                WorkspaceImportPreviewItem(
                    id = id,
                    title = text.first,
                    subtitle = selectable.changeInfo ?: text.second,
                    status = selectable.status.toPreviewItemStatus(),
                    isSelected = selectable.isSelected,
                    isSelectable = selectable.isSelectable,
                )
            },
    )

private fun com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.toPreviewItemStatus(): WorkspaceImportPreviewItemStatus =
    when (this) {
        com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.NEW -> WorkspaceImportPreviewItemStatus.New
        com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.UPDATED -> WorkspaceImportPreviewItemStatus.Updated
        com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus.DELETED -> WorkspaceImportPreviewItemStatus.Deleted
    }
