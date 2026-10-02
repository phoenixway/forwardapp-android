package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.sync.SyncRepository
import javax.inject.Inject

class SelectiveImportCoordinator @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend fun importSelection(state: SelectiveImportState): Result<Unit> {
        val content =
            state.backupContent
                ?: return Result.failure(IllegalStateException("Nothing to import"))

        val snapshotBundle =
            state.sourceSnapshotBundle
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Selective import requires a canonical SnapshotBundle source.",
                    ),
                )

        val effectiveSelection =
            state.selection.takeUnless { it.isEmpty() }
                ?: content.toWorkspaceSelectiveImportSelection()

        val filteredSnapshotBundle =
            runCatching {
                syncRepository.filterSnapshotBundleForSelectiveImport(
                    bundle = snapshotBundle,
                    selection = effectiveSelection,
                )
            }.getOrElse { error -> return Result.failure(error) }

        return syncRepository.importSelectedSnapshotBundle(filteredSnapshotBundle).map { Unit }
    }

}

private fun WorkspaceSelectiveImportSelection.isEmpty(): Boolean =
    selectedContextIds.isEmpty() &&
        selectedGoalIds.isEmpty() &&
        selectedWorkspaceBacklogEntryIds.isEmpty() &&
        selectedDocumentIds.isEmpty() &&
        selectedChecklistIds.isEmpty() &&
        selectedLinkItemIds.isEmpty() &&
        selectedInboxRecordIds.isEmpty() &&
        selectedContextLogIds.isEmpty() &&
        selectedScriptIds.isEmpty() &&
        selectedAttachmentIds.isEmpty() &&
        selectedActivityRecordIds.isEmpty()
