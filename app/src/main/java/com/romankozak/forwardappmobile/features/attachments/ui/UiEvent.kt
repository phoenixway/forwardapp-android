package com.romankozak.forwardappmobile.features.attachments.ui

import com.romankozak.forwardappmobile.core.navigation.NavTarget

sealed class UiEvent {
    data class Navigate(val target: NavTarget) : UiEvent()

    data class OpenUri(val uri: String) : UiEvent()
}
