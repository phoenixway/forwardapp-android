package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent

/** Типи дій з goalами */
enum class GoalActionType {
    CreateInstance,
    MoveInstance,
    CopyGoal,
    AddLinkToList,
    ADD_LIST_SHORTCUT,
}

/** Стан діалогу вибору дії з goalом */
sealed class GoalActionDialogState {
    data object Hidden : GoalActionDialogState()

    data class AwaitingActionChoice(val itemContent: BacklogItemContent) : GoalActionDialogState()
}
