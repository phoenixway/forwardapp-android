package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent

import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


sealed interface ClearCommand {
    data object Home : ClearCommand

    data object CloseSearch : ClearCommand

    data class NavigateToProject(val projectId: String, val projectName: String) : ClearCommand

    data object CollapseAll : ClearCommand
}


sealed class ClearResult {
    data object Success : ClearResult()

    data class Error(val message: String, val cause: Throwable? = null) : ClearResult()
}


data class ClearExecutionContext(
    val currentProjects: List<Project>,
    val subStateStack: StateFlow<List<MainSubState>>,
    val searchUseCase: com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SearchUseCase,
    val planningUseCase: com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.PlanningUseCase?,
    val planningModeManager: com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager?,
    val enhancedNavigationManager: EnhancedNavigationManager?,
    val uiEventChannel: Channel<ProjectUiEvent>,
)

@Singleton
class ClearAndNavigateHomeUseCase
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        companion object {
            private const val TAG = "ClearNavigateUseCase"
        }

        
        suspend fun execute(
            command: ClearCommand,
            context: ClearExecutionContext,
        ): ClearResult =
            try {
                Log.d(TAG, "Executing command: $command")

                when (command) {
                    is ClearCommand.Home -> executeHomeNavigation(context)
                    is ClearCommand.CloseSearch -> executeCloseSearch(context)
                    is ClearCommand.NavigateToProject -> executeProjectNavigation(command, context)
                    is ClearCommand.CollapseAll -> executeCollapseAll(context)
                }

                Log.d(TAG, "Command executed successfully: $command")
                ClearResult.Success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute command: $command", e)
                ClearResult.Error("Failed to execute clear command: ${command::class.simpleName}", e)
            }

        

        private suspend fun executeHomeNavigation(context: ClearExecutionContext) {
            clearUIStateToHome(context)
            collapseExpandedProjects(context.currentProjects)
            resetPlanningModeToDefault(context)
            navigateToHome(context)
            scrollToTop(context)
        }

        private suspend fun executeCloseSearch(context: ClearExecutionContext) {
            clearUIStateToHome(context)
            collapseExpandedProjects(context.currentProjects)
            resetPlanningModeToDefault(context)
            navigateToHome(context)
            scrollToTop(context)
        }

        private suspend fun executeProjectNavigation(
            command: ClearCommand.NavigateToProject,
            context: ClearExecutionContext,
        ) {
            clearSearchAndNavigation(context)
            resetSubStateToHierarchy(context)
            
            context.enhancedNavigationManager?.navigateToProject(command.projectId, command.projectName)
        }

        private suspend fun executeCollapseAll(context: ClearExecutionContext) {
            collapseExpandedProjects(context.currentProjects)
            clearSearchAndNavigation(context)
            resetSubStateToHierarchy(context)
            scrollToTop(context)
        }

        

        private suspend fun clearUIStateToHome(context: ClearExecutionContext) {
            withContext(Dispatchers.Main.immediate) {
                Log.d(TAG, "Clearing UI state to home")
                context.searchUseCase.popToSubState(MainSubState.Hierarchy)
                context.searchUseCase.clearAllSearchState()
                context.searchUseCase.clearNavigation()
            }
        }

        private suspend fun clearSearchAndNavigation(context: ClearExecutionContext) {
            withContext(Dispatchers.Main.immediate) {
                Log.d(TAG, "Clearing search and navigation")
                context.searchUseCase.clearAllSearchState()
                context.searchUseCase.clearNavigation()
            }
        }

        private suspend fun resetSubStateToHierarchy(context: ClearExecutionContext) {
            withContext(Dispatchers.Main.immediate) {
                context.searchUseCase.popToSubState(MainSubState.Hierarchy)
            }
        }

        private suspend fun collapseExpandedProjects(currentProjects: List<Project>) {
            withContext(ioDispatcher) {
                val expandedProjects = currentProjects.filter { it.isExpanded }
                Log.d(TAG, "Found ${expandedProjects.size} expanded projects to collapse")

                if (expandedProjects.isNotEmpty()) {
                    val collapsedProjects = expandedProjects.map { it.copy(isExpanded = false) }
                    projectRepository.updateProjects(collapsedProjects)
                    Log.d(TAG, "Collapsed ${collapsedProjects.size} projects")
                }
            }
        }

        private suspend fun resetPlanningModeToDefault(context: ClearExecutionContext) {
            withContext(Dispatchers.Main.immediate) {
                Log.d(TAG, "Resetting planning mode to default")
                context.planningUseCase?.let {
                    it.onPlanningModeChange(PlanningMode.All)
                    it.planningModeManager.resetExpansionStates()
                } ?: context.planningModeManager?.let {
                    it.changeMode(PlanningMode.All)
                    it.resetExpansionStates()
                }
            }
        }

        private suspend fun navigateToHome(context: ClearExecutionContext) {
            withContext(Dispatchers.Main.immediate) {
                Log.d(TAG, "Navigating to home")
                context.enhancedNavigationManager?.navigateHome()
            }
        }

        private suspend fun scrollToTop(context: ClearExecutionContext) {
            withContext(Dispatchers.Main.immediate) {
                Log.d(TAG, "Scrolling to top")
                context.uiEventChannel.trySend(ProjectUiEvent.ScrollToIndex(0))
            }
        }

        



        @Deprecated("Use execute() with ClearCommand instead")
        suspend operator fun invoke(
            currentProjects: List<Project>,
            onComplete: () -> Unit,
        ) {
            collapseExpandedProjects(currentProjects)
            withContext(Dispatchers.Main.immediate) {
                onComplete()
            }
        }

        @Deprecated("Use execute() with ClearCommand instead")
        suspend operator fun invoke(
            currentProjects: List<Project>,
            onSubStateCleared: () -> Unit,
            onNavigationCleared: () -> Unit,
            onNavigateHome: () -> Unit,
            onScrollToTop: () -> Unit,
        ) {
            withContext(Dispatchers.Main.immediate) {
                onSubStateCleared()
                onNavigationCleared()
            }

            collapseExpandedProjects(currentProjects)

            withContext(Dispatchers.Main.immediate) {
                onNavigateHome()
                onScrollToTop()
            }
        }
    }


fun createClearExecutionContext(
    currentProjects: List<Project>,
    subStateStack: StateFlow<List<MainSubState>>,
    searchUseCase: com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SearchUseCase,
    planningUseCase: com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.PlanningUseCase? = null,
    planningModeManager: com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager? = null,
    enhancedNavigationManager: EnhancedNavigationManager?,
    uiEventChannel: Channel<ProjectUiEvent>,
): ClearExecutionContext =
    ClearExecutionContext(
        currentProjects = currentProjects,
        subStateStack = subStateStack,
        searchUseCase = searchUseCase,
        planningUseCase = planningUseCase,
        planningModeManager = planningModeManager,
        enhancedNavigationManager = enhancedNavigationManager,
        uiEventChannel = uiEventChannel,
    )
