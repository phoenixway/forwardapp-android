package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

sealed interface MainScreenEvent {
    data class ShowCreateDialog(val parentId: String? = null) : MainScreenEvent
    data class ShowEditDialog(val projectId: String) : MainScreenEvent
    data class RequestDelete(val projectId: String) : MainScreenEvent
    data object HideDialog : MainScreenEvent
    data object CancelDeletion : MainScreenEvent
    data class SubmitProject(val name: String, val description: String) : MainScreenEvent
    data object ConfirmDeletion : MainScreenEvent
    data class ToggleProjectExpanded(val projectId: String) : MainScreenEvent
}
