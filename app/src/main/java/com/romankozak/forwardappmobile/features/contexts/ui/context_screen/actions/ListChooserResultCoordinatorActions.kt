package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import androidx.lifecycle.SavedStateHandle

class ListChooserResultCoordinatorActions(
    private val listChooserResultActions: ListChooserResultActions,
    private val pendingStateActions: ListChooserPendingStateActions,
) {
    sealed class Command {
        data class DirectionLink(val itemId: String, val linkedContextId: String?) : Command()

        data class ShowMessage(val message: String) : Command()

        data class AttachmentShare(
            val message: String,
            val newlyAddedItemId: String? = null,
            val shouldRefreshCurrentContext: Boolean = false,
        ) : Command()

        data class InboxPromotion(val targetContextId: String) : Command()

        data class PendingAction(val newlyAddedItemId: String? = null) : Command()

        data object None : Command()
    }

    data class ProcessOutput(
        val pendingState: ListChooserPendingStateActions.VmPendingState,
        val command: Command,
    )

    data class ProcessInput(
        val targetContextId: String,
        val currentContextId: String,
        val savedStateHandle: SavedStateHandle,
        val pendingState: ListChooserPendingStateActions.VmPendingState,
        val hasInboxPromotionRecord: Boolean,
        val clearDirectionPending: () -> Unit,
        val clearSelection: () -> Unit,
    )

    suspend fun process(input: ProcessInput): ProcessOutput {
        val snapshot =
            pendingStateActions.buildSnapshot(
                savedStateHandle = input.savedStateHandle,
                state = input.pendingState,
                hasInboxPromotionRecord = input.hasInboxPromotionRecord,
            )
        val execution =
            listChooserResultActions.resolveAndExecute(
                targetContextId = input.targetContextId,
                currentContextId = input.currentContextId,
                snapshot = snapshot,
            )

        var updatedState = input.pendingState
        val command =
            when (val outcome = execution.outcome) {
                is ListChooserResultActions.Outcome.DirectionLink ->
                    Command.DirectionLink(outcome.itemId, outcome.linkedContextId)
                is ListChooserResultActions.Outcome.DirectionAddCompleted ->
                    outcome.errorMessage?.let { Command.ShowMessage(it) } ?: Command.None
                is ListChooserResultActions.Outcome.AttachmentShareCompleted -> {
                    updatedState = updatedState.copy(pendingAttachmentShare = null)
                    Command.AttachmentShare(
                        message = outcome.message,
                        newlyAddedItemId = outcome.newlyAddedItemId,
                        shouldRefreshCurrentContext = outcome.shouldRefreshCurrentContext,
                    )
                }
                ListChooserResultActions.Outcome.InboxPromotion ->
                    Command.InboxPromotion(input.targetContextId)
                is ListChooserResultActions.Outcome.PendingActionCompleted ->
                    Command.PendingAction(newlyAddedItemId = outcome.newlyAddedItemId)
                ListChooserResultActions.Outcome.None -> Command.None
            }

        updatedState =
            pendingStateActions.applyCleanup(
                savedStateHandle = input.savedStateHandle,
                state = updatedState,
                cleanup = execution.cleanup,
                clearDirectionPending = input.clearDirectionPending,
                clearSelection = input.clearSelection,
            )

        return ProcessOutput(
            pendingState = updatedState,
            command = command,
        )
    }
}
