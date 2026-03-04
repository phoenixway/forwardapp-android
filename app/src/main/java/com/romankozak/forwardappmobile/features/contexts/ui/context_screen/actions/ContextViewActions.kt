package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.context.ContextCommand
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager

class ContextViewActions(
    private val contextSessionStore: ContextSessionStore,
    private val stateManager: ContextStateManager,
) {
    fun applyViewChange(mode: ContextViewMode): ContextViewMode {
        val session = contextSessionStore.dispatch(ContextCommand.SelectView(mode))
        val resolved = session.currentView
        stateManager.switchViewMode(resolved)
        stateManager.setInputMode(resolved.defaultInputMode())
        return resolved
    }
}

private fun ContextViewMode.defaultInputMode(): InputMode =
    when (this) {
        ContextViewMode.INBOX, ContextViewMode.ADVANCED -> InputMode.AddQuickRecord
        ContextViewMode.DIRECTION -> InputMode.AddDirection
        else -> InputMode.AddGoal
    }
