package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers
class ProjectNavigationHandler(
    private val contextRepository: ContextRepository,
    private val stateManager: ContextStateManager,
    private val uiEventFlow: MutableSharedFlow<UiEvent>,
    private val scope: CoroutineScope
) {
    fun onBackPressed() = scope.launch { uiEventFlow.emit(UiEvent.NavigateBack) }
    fun onHomeClick() = scope.launch { uiEventFlow.emit(UiEvent.Navigate(NavTarget.Home)) }
    fun onForwardPressed(id: String) = scope.launch { uiEventFlow.emit(NavTarget.ContextDetails(id)) }
    
    fun onCloseSearch() = stateManager.updateState { it.copy(searchQuery = "") }
    fun onProjectViewChange(mode: ContextViewMode) = stateManager.updateState { it.copy(currentView = mode) }
    fun onToggleProjectManagement() = stateManager.updateState { it.copy(isProjectManagementEnabled = !it.isProjectManagementEnabled) }

    fun deleteCurrentProject(id: String) = scope.launch { 
        contextRepository.deleteContext(id)
        uiEventFlow.emit(UiEvent.NavigateBack)
    }
    fun addCurrentProjectToDayPlan(id: String) = scope.launch { /* логіка */ }
    fun onAddMilestone(id: String) = stateManager.updateState { it.copy(showAddMilestoneDialog = true) }
}

