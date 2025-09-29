package com.romankozak.forwardappmobile.ui.screens.attachments

sealed class UiEvent {
    data class Navigate(val route: String) : UiEvent()
    data class OpenUri(val uri: String) : UiEvent()
    data class NavigateToListChooser(val title: String, val disabledIds: String) : UiEvent()
}
