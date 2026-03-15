package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.DirectionChooserActions.AddDirectionRequest
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.DirectionChooserActions.LinkDirectionRequest
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType

class ListChooserPendingStateActions {
    data class VmPendingState(
        val pendingDirectionLinkItemId: String? = null,
        val pendingAddDirectionFromContextChooser: Boolean = false,
        val pendingAttachmentShare: BacklogItemContent? = null,
    )

    fun savePendingAction(
        savedStateHandle: SavedStateHandle,
        actionType: GoalActionType,
        itemIds: Set<String>,
        goalIds: Set<String>,
    ) {
        savedStateHandle[KEY_PENDING_ACTION] = actionType.name
        savedStateHandle[KEY_PENDING_SOURCE_ITEM_IDS] = itemIds.toList()
        savedStateHandle[KEY_PENDING_SOURCE_GOAL_IDS] = goalIds.toList()
    }

    fun saveDirectionAddRequest(
        savedStateHandle: SavedStateHandle,
        request: AddDirectionRequest,
        currentState: VmPendingState,
    ): VmPendingState =
        VmPendingState(
            pendingDirectionLinkItemId = currentState.pendingDirectionLinkItemId,
            pendingAddDirectionFromContextChooser = request.pendingAddDirectionFromContextChooser,
            pendingAttachmentShare = currentState.pendingAttachmentShare,
        ).also {
            savedStateHandle[KEY_PENDING_ADD_DIRECTION_FROM_CHOOSER] =
                request.savedPendingAddDirectionFromContextChooser
        }

    fun saveDirectionLinkRequest(
        savedStateHandle: SavedStateHandle,
        request: LinkDirectionRequest,
        currentState: VmPendingState,
    ): VmPendingState =
        VmPendingState(
            pendingDirectionLinkItemId = request.pendingDirectionLinkItemId,
            pendingAddDirectionFromContextChooser = currentState.pendingAddDirectionFromContextChooser,
            pendingAttachmentShare = currentState.pendingAttachmentShare,
        ).also {
            savedStateHandle[KEY_PENDING_DIRECTION_LINK_ITEM_ID] = request.pendingDirectionLinkItemId
            savedStateHandle[KEY_PENDING_DIRECTION_LINK] = request.savedPendingDirectionLink
        }

    fun buildSnapshot(
        savedStateHandle: SavedStateHandle,
        state: VmPendingState,
        hasInboxPromotionRecord: Boolean,
    ): ListChooserResultActions.PendingSnapshot =
        ListChooserResultActions.PendingSnapshot(
            pendingDirectionLinkItemId = state.pendingDirectionLinkItemId,
            pendingAddDirectionFromContextChooser =
                state.pendingAddDirectionFromContextChooser,
            savedPendingAddDirectionFromContextChooser =
                savedStateHandle.get<Boolean>(KEY_PENDING_ADD_DIRECTION_FROM_CHOOSER) == true,
            pendingAttachmentShare = state.pendingAttachmentShare,
            hasInboxPromotionRecord = hasInboxPromotionRecord,
            pendingActionTypeName = savedStateHandle.get<String>(KEY_PENDING_ACTION),
            pendingSourceItemIds =
                savedStateHandle.get<List<String>>(KEY_PENDING_SOURCE_ITEM_IDS) ?: emptyList(),
            pendingSourceGoalIds =
                savedStateHandle.get<List<String>>(KEY_PENDING_SOURCE_GOAL_IDS) ?: emptyList(),
        )

    fun applyCleanup(
        savedStateHandle: SavedStateHandle,
        state: VmPendingState,
        cleanup: ListChooserResultActions.Cleanup,
        clearDirectionPending: () -> Unit,
        clearSelection: () -> Unit,
    ): VmPendingState {
        var updated = state
        if (cleanup.clearDirectionLink) {
            clearDirectionPending()
            savedStateHandle.remove<String>(KEY_PENDING_DIRECTION_LINK_ITEM_ID)
            savedStateHandle.remove<Boolean>(KEY_PENDING_DIRECTION_LINK)
            updated = updated.copy(pendingDirectionLinkItemId = null)
        }
        if (cleanup.clearDirectionAdd) {
            clearDirectionPending()
            savedStateHandle.remove<Boolean>(KEY_PENDING_ADD_DIRECTION_FROM_CHOOSER)
            updated = updated.copy(pendingAddDirectionFromContextChooser = false)
        }
        if (cleanup.clearPendingAction) {
            savedStateHandle.remove<String>(KEY_PENDING_ACTION)
            savedStateHandle.remove<List<String>>(KEY_PENDING_SOURCE_ITEM_IDS)
            savedStateHandle.remove<List<String>>(KEY_PENDING_SOURCE_GOAL_IDS)
        }
        if (cleanup.clearSelection) clearSelection()
        return updated
    }

    companion object {
        const val KEY_PENDING_ACTION = "pendingAction"
        const val KEY_PENDING_SOURCE_ITEM_IDS = "pendingSourceItemIds"
        const val KEY_PENDING_SOURCE_GOAL_IDS = "pendingSourceGoalIds"
        const val KEY_PENDING_ADD_DIRECTION_FROM_CHOOSER = "pendingAddDirectionFromContextChooser"
        const val KEY_PENDING_DIRECTION_LINK_ITEM_ID = "pendingDirectionLinkItemId"
        const val KEY_PENDING_DIRECTION_LINK = "pendingDirectionLink"
    }
}
