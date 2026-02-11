package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow

class UiEventDispatcherActions(
    private val uiEventFlow: MutableSharedFlow<UiEvent>,
) {
    suspend fun emit(event: UiEvent) {
        uiEventFlow.emit(event)
    }

    fun tryEmit(event: UiEvent) {
        uiEventFlow.tryEmit(event)
    }

    suspend fun emitAll(events: List<UiEvent>) {
        events.forEach { event -> uiEventFlow.emit(event) }
    }
}
