package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.context.ContextCommand
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProjectNavigationHandler(
    private val contextRepository: ContextRepository,
    private val stateManager: ContextStateManager,
    private val contextSessionStore: ContextSessionStore,
    private val resultListener: ResultListener,
    private val scope: CoroutineScope,
) {
    sealed interface Command {
        data object Back : Command

        data object Home : Command

        data class Forward(
            val id: String,
        ) : Command

        data object CloseSearch : Command

        data class ChangeView(
            val mode: ContextViewMode,
        ) : Command

        data object ToggleProjectManagement : Command

        data class DeleteProject(
            val id: String,
        ) : Command

        data class AddMilestone(
            val id: String,
        ) : Command
    }

    interface ResultListener {
        fun onBackPressed(): Boolean

        fun onHomeClick()

        fun onForwardPressed(id: String)

        fun deleteCurrentProject(id: String)
    }

    fun dispatch(command: Command) {
        when (command) {
            Command.Back -> onBackPressed()
            Command.Home -> onHomeClick()
            is Command.Forward -> onForwardPressed(command.id)
            Command.CloseSearch -> onCloseSearch()
            is Command.ChangeView -> onProjectViewChange(command.mode)
            Command.ToggleProjectManagement -> onToggleProjectManagement()
            is Command.DeleteProject -> deleteCurrentProject(command.id)
            is Command.AddMilestone -> onAddMilestone(command.id)
        }
    }

    fun onBackPressed() = resultListener.onBackPressed()

    fun onHomeClick() = resultListener.onHomeClick()

    fun onForwardPressed(id: String) = resultListener.onForwardPressed(id)

    fun onCloseSearch() = stateManager.updateState { it.copy(searchQuery = "") }

    fun onProjectViewChange(mode: ContextViewMode) {
        val session = contextSessionStore.dispatch(ContextCommand.SelectView(mode))
        stateManager.updateState { it.copy(currentViewMode = session.currentView) }
    }

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
