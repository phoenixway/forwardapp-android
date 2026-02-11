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
                state =
                    ListChooserOrchestrationActions.PendingState(
                        pendingDirectionLinkItemId = snapshot.pendingDirectionLinkItemId,
                        pendingAddDirectionFromContextChooser = snapshot.pendingAddDirectionFromContextChooser,
                        savedPendingAddDirectionFromContextChooser = snapshot.savedPendingAddDirectionFromContextChooser,
                        pendingAttachmentShare = snapshot.pendingAttachmentShare,
                        hasInboxPromotionRecord = snapshot.hasInboxPromotionRecord,
                        pendingActionTypeName = snapshot.pendingActionTypeName,
                        pendingSourceItemIds = snapshot.pendingSourceItemIds,
                        pendingSourceGoalIds = snapshot.pendingSourceGoalIds,
                    ),
            )

        return when (nextStep) {
            is ListChooserOrchestrationActions.NextStep.DirectionLink ->
                ExecutionResult(
                    outcome =
                        Outcome.DirectionLink(
                            itemId = nextStep.itemId,
                            linkedContextId = nextStep.linkedContextId,
                        ),
                    cleanup = Cleanup(clearDirectionLink = true),
                )

            is ListChooserOrchestrationActions.NextStep.DirectionAdd -> {
                val result =
                    flowActions.addDirectionLinkedToContext(
                        targetContextId = targetContextId,
                        currentContextId = currentContextId,
                    )
                ExecutionResult(
                    outcome = Outcome.DirectionAddCompleted(errorMessage = result.errorMessage),
                    cleanup = Cleanup(clearDirectionAdd = true),
                )
            }

            is ListChooserOrchestrationActions.NextStep.AttachmentShare -> {
                val result =
                    flowActions.shareAttachmentToProject(
                        attachment = nextStep.attachment,
                        targetContextId = targetContextId,
                        currentContextId = currentContextId,
                    )
                ExecutionResult(
                    outcome =
                        Outcome.AttachmentShareCompleted(
                            message = result.message,
                            newlyAddedItemId = result.newlyAddedItemId,
                            shouldRefreshCurrentContext = result.shouldRefreshCurrentContext,
                        ),
                )
            }

            is ListChooserOrchestrationActions.NextStep.InboxPromotion ->
                ExecutionResult(outcome = Outcome.InboxPromotion)

            is ListChooserOrchestrationActions.NextStep.PendingAction -> {
                val result =
                    listChooserActions.executePendingAction(
                        actionType = nextStep.actionType,
                        targetContextId = targetContextId,
                        currentContextId = currentContextId,
                        itemIds = nextStep.itemIds,
                        goalIds = nextStep.goalIds,
                    )
                ExecutionResult(
                    outcome = Outcome.PendingActionCompleted(newlyAddedItemId = result.newlyAddedItemId),
                    cleanup = Cleanup(clearPendingAction = true, clearSelection = true),
                )
            }

            ListChooserOrchestrationActions.NextStep.None ->
                ExecutionResult(outcome = Outcome.None)
        }
    }
}
