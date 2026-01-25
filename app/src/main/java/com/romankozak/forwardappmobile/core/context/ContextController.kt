package com.romankozak.forwardappmobile.core.context

interface ContextController {
    fun current(): ContextState

    fun update(block: (ContextState) -> ContextState)
}

class DefaultContextController(
    initial: ContextState,
) : ContextController {
    private var state = initial

    override fun current() = state

    override fun update(block: (ContextState) -> ContextState) {
        state = block(state)
    }
}
