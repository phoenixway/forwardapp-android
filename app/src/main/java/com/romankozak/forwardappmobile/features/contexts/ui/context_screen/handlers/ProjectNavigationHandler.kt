package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProjectNavigationHandler(
    private val contextRepository: ContextRepository,
    private val stateManager: ContextStateManager,
    private val resultListener: ResultListener,
    private val scope: CoroutineScope,
) {
    interface ResultListener {
        fun onBackPressed(): Boolean

        fun onHomeClick()

        fun onForwardPressed(id: String)

        fun deleteCurrentProject(id: String)
    }

    fun onBackPressed() = resultListener.onBackPressed()

    fun onHomeClick() = resultListener.onHomeClick()

    fun onForwardPressed(id: String) = resultListener.onForwardPressed(id)

    fun onCloseSearch() = stateManager.updateState { it.copy(searchQuery = "") }

    fun onProjectViewChange(mode: ContextViewMode) = stateManager.updateState { it.copy(currentViewMode = mode) }

    fun onToggleProjectManagement() = stateManager.updateState { it.copy(isProjectManagementEnabled = !it.isProjectManagementEnabled) }

    fun deleteCurrentProject(id: String) = resultListener.deleteCurrentProject(id)

    fun addCurrentProjectToDayPlan(id: String) = scope.launch { /* TODO */ }

    fun onAddMilestone(id: String) =
        stateManager.updateState {
            it.copy(
                showCreateNoteDocumentDialog = true,
            )
        } // TODO check what dialog to show
}
