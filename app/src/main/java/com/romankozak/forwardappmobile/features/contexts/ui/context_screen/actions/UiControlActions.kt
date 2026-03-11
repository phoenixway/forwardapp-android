package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ContextManagementTab
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager

class UiControlActions(
    private val stateManager: ContextStateManager,
    private val contextSessionStore: ContextSessionStore,
) {
    fun selectDashboardTab(tab: ContextManagementTab) = stateManager.switchTab(tab)

    fun toggleSearchMode() = stateManager.toggleSearchMode()

    fun updateSearchQuery(query: String) = stateManager.updateSearchQuery(query)

    fun dismissDisplayPropertiesDialog() = stateManager.dismissDisplayPropertiesDialog()

    fun showDisplayPropertiesDialog() = stateManager.showDisplayPropertiesDialog()

    fun hasCapability(capabilityId: CapabilityId): Boolean = contextSessionStore.state.value.enabledCapabilities.contains(capabilityId)

    fun forceRefresh() {
        stateManager.updateState {
            it.copy(
                refreshTrigger = it.refreshTrigger + 1,
                needsStateRefresh = true,
            )
        }
    }
}
