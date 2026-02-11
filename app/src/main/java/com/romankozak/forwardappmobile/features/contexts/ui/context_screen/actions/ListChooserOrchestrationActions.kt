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
    ): NextStep {
        state.pendingDirectionLinkItemId?.let { itemId ->
            return NextStep.DirectionLink(
                itemId = itemId,
                linkedContextId = targetContextId.takeIf { it != "root" },
            )
        }

        val pendingDirectionAdd =
            state.pendingAddDirectionFromContextChooser || state.savedPendingAddDirectionFromContextChooser
        if (pendingDirectionAdd) return NextStep.DirectionAdd

        state.pendingAttachmentShare?.let { attachment ->
            return NextStep.AttachmentShare(attachment)
        }

        if (state.hasInboxPromotionRecord) return NextStep.InboxPromotion

        val actionTypeName = state.pendingActionTypeName ?: return NextStep.None
        val actionType = GoalActionType.valueOf(actionTypeName)
        return NextStep.PendingAction(
            actionType = actionType,
            itemIds = state.pendingSourceItemIds,
            goalIds = state.pendingSourceGoalIds,
        )
    }
}
