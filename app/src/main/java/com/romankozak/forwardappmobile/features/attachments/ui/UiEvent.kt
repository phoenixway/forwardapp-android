package com.romankozak.forwardappmobile.features.attachments.ui

import com.romankozak.forwardappmobile.features.navigation.NavTarget

sealed class UiEvent {
    data class Navigate(val target: NavTarget) : UiEvent()
    data class OpenUri(val uri: String) : UiEvent()
}
