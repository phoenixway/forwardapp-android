package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent

class ListChooserResultActions(
    private val orchestrationActions: ListChooserOrchestrationActions,
    private val flowActions: ListChooserFlowActions,
    private val listChooserActions: ListChooserActions,
) {
    data class PendingSnapshot(
        val pendingDirectionLinkItemId: String?,
        val pendingAddDirectionFromContextChooser: Boolean,
        val savedPendingAddDirectionFromContextChooser: Boolean,
        val pendingAttachmentShare: BacklogItemContent?,
        val hasInboxPromotionRecord: Boolean,
        val pendingActionTypeName: String?,
        val pendingSourceItemIds: List<String>,
        val pendingSourceGoalIds: List<String>,
    )

    data class Cleanup(
        val clearDirectionLink: Boolean = false,
        val clearDirectionAdd: Boolean = false,
        val clearPendingAction: Boolean = false,
        val clearSelection: Boolean = false,
    )

    data class ExecutionResult(
        val outcome: Outcome,
        val cleanup: Cleanup = Cleanup(),
    )

    sealed class Outcome {
        data class DirectionLink(
            val itemId: String,
            val linkedContextId: String?,
        ) : Outcome()

        data class DirectionAddCompleted(
            val errorMessage: String? = null,
        ) : Outcome()

        data class AttachmentShareCompleted(
            val message: String,
            val newlyAddedItemId: String? = null,
            val shouldRefreshCurrentContext: Boolean = false,
        ) : Outcome()

        data object InboxPromotion : Outcome()

        data class PendingActionCompleted(
            val newlyAddedItemId: String? = null,
            val userMessage: String? = null,
        ) : Outcome()

        data object None : Outcome()
    }

    suspend fun resolveAndExecute(
        targetContextId: String,
        currentContextId: String,
        snapshot: PendingSnapshot,
    ): ExecutionResult {
        val nextStep =
            orchestrationActions.resolveNextStep(
                targetContextId = targetContextId,
                state = snapshot.toPendingState(),
            )

        return when (nextStep) {
            is ListChooserOrchestrationActions.NextStep.DirectionLink -> nextStep.toDirectionLinkResult()
            is ListChooserOrchestrationActions.NextStep.DirectionAdd ->
                handleDirectionAdd(targetContextId, currentContextId)
            is ListChooserOrchestrationActions.NextStep.AttachmentShare ->
                handleAttachmentShare(nextStep, targetContextId, currentContextId)
            is ListChooserOrchestrationActions.NextStep.InboxPromotion ->
                ExecutionResult(outcome = Outcome.InboxPromotion)
            is ListChooserOrchestrationActions.NextStep.PendingAction ->
                handlePendingAction(nextStep, targetContextId, currentContextId)
            ListChooserOrchestrationActions.NextStep.None -> ExecutionResult(outcome = Outcome.None)
        }
    }

    private fun PendingSnapshot.toPendingState(): ListChooserOrchestrationActions.PendingState {
        return ListChooserOrchestrationActions.PendingState(
            pendingDirectionLinkItemId = pendingDirectionLinkItemId,
            pendingAddDirectionFromContextChooser = pendingAddDirectionFromContextChooser,
            savedPendingAddDirectionFromContextChooser = savedPendingAddDirectionFromContextChooser,
            pendingAttachmentShare = pendingAttachmentShare,
            hasInboxPromotionRecord = hasInboxPromotionRecord,
            pendingActionTypeName = pendingActionTypeName,
            pendingSourceItemIds = pendingSourceItemIds,
            pendingSourceGoalIds = pendingSourceGoalIds,
        )
    }

    private fun ListChooserOrchestrationActions.NextStep.DirectionLink.toDirectionLinkResult(): ExecutionResult {
        return ExecutionResult(
            outcome =
                Outcome.DirectionLink(
                    itemId = itemId,
                    linkedContextId = linkedContextId,
                ),
            cleanup = Cleanup(clearDirectionLink = true),
        )
    }

    private suspend fun handleDirectionAdd(
        targetContextId: String,
        currentContextId: String,
    ): ExecutionResult {
        val result =
            flowActions.addDirectionLinkedToContext(
                targetContextId = targetContextId,
                currentContextId = currentContextId,
            )
        return ExecutionResult(
            outcome = Outcome.DirectionAddCompleted(errorMessage = result.errorMessage),
            cleanup = Cleanup(clearDirectionAdd = true),
        )
    }

    private suspend fun handleAttachmentShare(
        nextStep: ListChooserOrchestrationActions.NextStep.AttachmentShare,
        targetContextId: String,
        currentContextId: String,
    ): ExecutionResult {
        val result =
            flowActions.shareAttachmentToProject(
                attachment = nextStep.attachment,
                targetContextId = targetContextId,
                currentContextId = currentContextId,
            )
        return ExecutionResult(
            outcome =
                Outcome.AttachmentShareCompleted(
                    message = result.message,
                    newlyAddedItemId = result.newlyAddedItemId,
                    shouldRefreshCurrentContext = result.shouldRefreshCurrentContext,
                ),
        )
    }

    private suspend fun handlePendingAction(
        nextStep: ListChooserOrchestrationActions.NextStep.PendingAction,
        targetContextId: String,
        currentContextId: String,
    ): ExecutionResult {
        val result =
            listChooserActions.executePendingAction(
                actionType = nextStep.actionType,
                targetContextId = targetContextId,
                currentContextId = currentContextId,
                itemIds = nextStep.itemIds,
                goalIds = nextStep.goalIds,
            )
        return ExecutionResult(
            outcome =
                Outcome.PendingActionCompleted(
                    newlyAddedItemId = result.newlyAddedItemId,
                    userMessage = result.userMessage,
                ),
            cleanup = Cleanup(clearPendingAction = true, clearSelection = true),
        )
    }
}
