package com.romankozak.forwardappmobile.ui.screens.attachments

import com.romankozak.forwardappmobile.ui.navigation.NavTarget

sealed class UiEvent {
    data class Navigate(val target: NavTarget) : UiEvent()
    data class OpenUri(val uri: String) : UiEvent()
}
