package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType

class ListChooserOrchestrationActions {
    data class PendingState(
        val pendingDirectionLinkItemId: String?,
        val pendingAddDirectionFromContextChooser: Boolean,
        val savedPendingAddDirectionFromContextChooser: Boolean,
        val pendingAttachmentShare: BacklogItemContent?,
        val hasInboxPromotionRecord: Boolean,
        val pendingActionTypeName: String?,
        val pendingSourceItemIds: List<String>,
        val pendingSourceGoalIds: List<String>,
    )

    sealed class NextStep {
        data class DirectionLink(
            val itemId: String,
            val linkedContextId: String?,
        ) : NextStep()

        data object DirectionAdd : NextStep()

        data class AttachmentShare(val attachment: BacklogItemContent) : NextStep()

        data object InboxPromotion : NextStep()

        data class PendingAction(
            val actionType: GoalActionType,
            val itemIds: List<String>,
            val goalIds: List<String>,
        ) : NextStep()

        data object None : NextStep()
    }

    fun resolveNextStep(
        targetContextId: String,
        state: PendingState,
    ): NextStep =
        when {
            state.pendingDirectionLinkItemId != null ->
                NextStep.DirectionLink(
                    itemId = state.pendingDirectionLinkItemId,
                    linkedContextId = targetContextId.takeIf { it != "root" },
                )

            state.pendingAddDirectionFromContextChooser ||
                state.savedPendingAddDirectionFromContextChooser -> {
                NextStep.DirectionAdd
            }

            state.pendingAttachmentShare != null -> {
                NextStep.AttachmentShare(state.pendingAttachmentShare)
            }

            state.hasInboxPromotionRecord -> NextStep.InboxPromotion

            state.pendingActionTypeName != null ->
                NextStep.PendingAction(
                    actionType = GoalActionType.valueOf(state.pendingActionTypeName),
                    itemIds = state.pendingSourceItemIds,
                    goalIds = state.pendingSourceGoalIds,
                )

            else -> NextStep.None
        }
}
