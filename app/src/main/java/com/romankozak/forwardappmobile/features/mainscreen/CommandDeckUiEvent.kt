package com.romankozak.forwardappmobile.features.mainscreen

sealed interface CommandDeckUiEvent {
    data class ShowMessage(val message: String) : CommandDeckUiEvent

    data class NavigateToSyncScreenWithData(val json: String) : CommandDeckUiEvent
}
