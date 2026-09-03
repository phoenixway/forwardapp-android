package com.romankozak.forwardappmobile.shared.application.imports

import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportDescriptor
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewModel
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSectionKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WorkspaceImportSessionStore {
    private val mutableState = MutableStateFlow(WorkspaceImportSessionState())
    val state: StateFlow<WorkspaceImportSessionState> = mutableState.asStateFlow()

    fun dispatch(intent: WorkspaceImportSessionIntent) {
        mutableState.update { current ->
            when (intent) {
                WorkspaceImportSessionIntent.PreviewLoadingStarted ->
                    current.copy(
                        isBusy = true,
                        phase = WorkspaceImportSessionPhase.LoadingPreview,
                        errorMessage = null,
                        pendingEffect = null,
                    )

                is WorkspaceImportSessionIntent.PreviewLoaded ->
                    current.copy(
                        isBusy = false,
                        phase = WorkspaceImportSessionPhase.Idle,
                        descriptor = intent.descriptor,
                        previewModel = WorkspaceImportPreviewModel(),
                        previewSummary = WorkspaceImportPreviewSummary(),
                        selection = WorkspaceSelectiveImportSelection(),
                        errorMessage = null,
                        pendingEffect = null,
                    )

                is WorkspaceImportSessionIntent.PreviewFailed ->
                    current.copy(
                        isBusy = false,
                        phase = WorkspaceImportSessionPhase.Idle,
                        errorMessage = intent.message,
                        pendingEffect = null,
                    )

                WorkspaceImportSessionIntent.ImportStarted ->
                    current.copy(
                        isBusy = true,
                        phase = WorkspaceImportSessionPhase.Importing,
                        errorMessage = null,
                        pendingEffect = null,
                    )

                WorkspaceImportSessionIntent.ImportSucceeded ->
                    current.copy(
                        isBusy = false,
                        phase = WorkspaceImportSessionPhase.Idle,
                        errorMessage = null,
                        pendingEffect = WorkspaceImportSessionEffect.NavigateBack,
                    )

                is WorkspaceImportSessionIntent.ImportFailed ->
                    current.copy(
                        isBusy = false,
                        phase = WorkspaceImportSessionPhase.Idle,
                        errorMessage = intent.message,
                        pendingEffect = null,
                    )

                WorkspaceImportSessionIntent.ErrorConsumed ->
                    current.copy(errorMessage = null)

                is WorkspaceImportSessionIntent.SelectionReplaced ->
                    current.copy(selection = intent.selection)

                is WorkspaceImportSessionIntent.ItemSelectionChanged ->
                    current.copy(
                        selection = current.selection.withItemSelection(intent.kind, intent.itemId, intent.isSelected),
                        previewSummary = current.previewSummary.withItemSelection(intent.kind, intent.isSelected),
                    )

                is WorkspaceImportSessionIntent.SectionSelectionChanged ->
                    current.copy(
                        selection =
                            current.selection.withSectionSelection(
                                kind = intent.kind,
                                itemIds = intent.itemIds,
                                isSelected = intent.isSelected,
                            ),
                        previewSummary =
                            current.previewSummary.withSectionSelection(
                                kind = intent.kind,
                                selectedCount = if (intent.isSelected) intent.itemIds.size else 0,
                            ),
                    )

                is WorkspaceImportSessionIntent.PreviewSummaryReplaced ->
                    current.copy(previewSummary = intent.summary)

                is WorkspaceImportSessionIntent.PreviewModelReplaced ->
                    current.copy(previewModel = intent.model)

                WorkspaceImportSessionIntent.EffectConsumed ->
                    current.copy(pendingEffect = null)
            }
        }
    }
}

data class WorkspaceImportSessionState(
    val isBusy: Boolean = false,
    val phase: WorkspaceImportSessionPhase = WorkspaceImportSessionPhase.Idle,
    val descriptor: WorkspaceImportDescriptor? = null,
    val previewModel: WorkspaceImportPreviewModel = WorkspaceImportPreviewModel(),
    val previewSummary: WorkspaceImportPreviewSummary = WorkspaceImportPreviewSummary(),
    val selection: WorkspaceSelectiveImportSelection = WorkspaceSelectiveImportSelection(),
    val errorMessage: String? = null,
    val pendingEffect: WorkspaceImportSessionEffect? = null,
)

enum class WorkspaceImportSessionPhase {
    Idle,
    LoadingPreview,
    Importing,
}

sealed interface WorkspaceImportSessionIntent {
    data object PreviewLoadingStarted : WorkspaceImportSessionIntent

    data class PreviewLoaded(
        val descriptor: WorkspaceImportDescriptor,
    ) : WorkspaceImportSessionIntent

    data class PreviewFailed(
        val message: String,
    ) : WorkspaceImportSessionIntent

    data object ImportStarted : WorkspaceImportSessionIntent
    data object ImportSucceeded : WorkspaceImportSessionIntent

    data class ImportFailed(
        val message: String,
    ) : WorkspaceImportSessionIntent

    data class SelectionReplaced(
        val selection: WorkspaceSelectiveImportSelection,
    ) : WorkspaceImportSessionIntent

    data class ItemSelectionChanged(
        val kind: WorkspaceImportPreviewSectionKind,
        val itemId: String,
        val isSelected: Boolean,
    ) : WorkspaceImportSessionIntent

    data class SectionSelectionChanged(
        val kind: WorkspaceImportPreviewSectionKind,
        val itemIds: Set<String>,
        val isSelected: Boolean,
    ) : WorkspaceImportSessionIntent

    data class PreviewSummaryReplaced(
        val summary: WorkspaceImportPreviewSummary,
    ) : WorkspaceImportSessionIntent

    data class PreviewModelReplaced(
        val model: WorkspaceImportPreviewModel,
    ) : WorkspaceImportSessionIntent

    data object ErrorConsumed : WorkspaceImportSessionIntent
    data object EffectConsumed : WorkspaceImportSessionIntent
}

sealed interface WorkspaceImportSessionEffect {
    data object NavigateBack : WorkspaceImportSessionEffect
}

private fun WorkspaceSelectiveImportSelection.withItemSelection(
    kind: WorkspaceImportPreviewSectionKind,
    itemId: String,
    isSelected: Boolean,
): WorkspaceSelectiveImportSelection =
    when (kind) {
        WorkspaceImportPreviewSectionKind.Contexts -> copy(selectedContextIds = selectedContextIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.Goals -> copy(selectedGoalIds = selectedGoalIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.Backlog ->
            copy(
                selectedWorkspaceBacklogEntryIds =
                    selectedWorkspaceBacklogEntryIds.withItem(itemId, isSelected),
            )
        WorkspaceImportPreviewSectionKind.LegacyNotes -> this
        WorkspaceImportPreviewSectionKind.ActivityRecords ->
            copy(selectedActivityRecordIds = selectedActivityRecordIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.Documents ->
            copy(selectedDocumentIds = selectedDocumentIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.Checklists ->
            copy(selectedChecklistIds = selectedChecklistIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.LinkItems ->
            copy(selectedLinkItemIds = selectedLinkItemIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.InboxRecords ->
            copy(selectedInboxRecordIds = selectedInboxRecordIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.ContextLogs ->
            copy(selectedContextLogIds = selectedContextLogIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.Scripts ->
            copy(selectedScriptIds = selectedScriptIds.withItem(itemId, isSelected))
        WorkspaceImportPreviewSectionKind.Attachments ->
            copy(selectedAttachmentIds = selectedAttachmentIds.withItem(itemId, isSelected))
    }

private fun WorkspaceSelectiveImportSelection.withSectionSelection(
    kind: WorkspaceImportPreviewSectionKind,
    itemIds: Set<String>,
    isSelected: Boolean,
): WorkspaceSelectiveImportSelection =
    when (kind) {
        WorkspaceImportPreviewSectionKind.Contexts -> copy(selectedContextIds = selectedContextIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.Goals -> copy(selectedGoalIds = selectedGoalIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.Backlog ->
            copy(
                selectedWorkspaceBacklogEntryIds =
                    selectedWorkspaceBacklogEntryIds.withItems(itemIds, isSelected),
            )
        WorkspaceImportPreviewSectionKind.LegacyNotes -> this
        WorkspaceImportPreviewSectionKind.ActivityRecords ->
            copy(selectedActivityRecordIds = selectedActivityRecordIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.Documents ->
            copy(selectedDocumentIds = selectedDocumentIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.Checklists ->
            copy(selectedChecklistIds = selectedChecklistIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.LinkItems ->
            copy(selectedLinkItemIds = selectedLinkItemIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.InboxRecords ->
            copy(selectedInboxRecordIds = selectedInboxRecordIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.ContextLogs ->
            copy(selectedContextLogIds = selectedContextLogIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.Scripts ->
            copy(selectedScriptIds = selectedScriptIds.withItems(itemIds, isSelected))
        WorkspaceImportPreviewSectionKind.Attachments ->
            copy(selectedAttachmentIds = selectedAttachmentIds.withItems(itemIds, isSelected))
    }

private fun WorkspaceImportPreviewSummary.withItemSelection(
    kind: WorkspaceImportPreviewSectionKind,
    isSelected: Boolean,
): WorkspaceImportPreviewSummary =
    copy(
        sections =
            sections.map { section ->
                if (section.kind == kind) {
                    section.copy(
                        selectedCount =
                            if (isSelected) {
                                (section.selectedCount + 1).coerceAtMost(section.totalCount)
                            } else {
                                (section.selectedCount - 1).coerceAtLeast(0)
                            },
                    )
                } else {
                    section
                }
            },
    )

private fun WorkspaceImportPreviewSummary.withSectionSelection(
    kind: WorkspaceImportPreviewSectionKind,
    selectedCount: Int,
): WorkspaceImportPreviewSummary =
    copy(
        sections =
            sections.map { section ->
                if (section.kind == kind) {
                    section.copy(selectedCount = selectedCount.coerceIn(0, section.totalCount))
                } else {
                    section
                }
            },
    )

private fun Set<String>.withItem(
    itemId: String,
    isSelected: Boolean,
): Set<String> = if (isSelected) this + itemId else this - itemId

private fun Set<String>.withItems(
    itemIds: Set<String>,
    isSelected: Boolean,
): Set<String> = if (isSelected) this + itemIds else this - itemIds
