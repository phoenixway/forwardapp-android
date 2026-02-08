package com.romankozak.forwardappmobile.core.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ContextController {
    val state: StateFlow<ContextState>

    fun current(): ContextState = state.value

    fun update(block: (ContextState) -> ContextState)

    fun set(state: ContextState)
}

class DefaultContextController(
    initial: ContextState,
) : ContextController {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<ContextState> = _state

    override fun update(block: (ContextState) -> ContextState) {
        _state.value = block(_state.value)
    }

    override fun set(state: ContextState) {
        _state.value = state
    }
}
