package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode

sealed interface CommandDeckUiEvent {
    data class ShowMessage(val message: String) : CommandDeckUiEvent

    data class ShowSessionModeChanged(
        val message: String,
        val previousMode: SessionMode?,
        val newMode: SessionMode,
    ) : CommandDeckUiEvent

    data class NavigateToSyncScreenWithData(val json: String) : CommandDeckUiEvent
}
