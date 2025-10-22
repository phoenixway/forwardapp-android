package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.ui.navigation.ClearCommand
import com.romankozak.forwardappmobile.ui.navigation.ClearResult
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.navigation.createClearExecutionContext
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class NavigationUseCase @Inject constructor(
    private val clearAndNavigateHomeUseCase: ClearAndNavigateHomeUseCase,
    private val searchUseCase: SearchUseCase,
    private val planningUseCase: PlanningUseCase,
) {
    var enhancedNavigationManager: EnhancedNavigationManager? = null
    var uiEventChannel: Channel<ProjectUiEvent>? = null
    var allProjectsFlat: StateFlow<List<Project>>? = null

    private val _isProcessingReveal = MutableStateFlow(false)
    val isProcessingReveal: StateFlow<Boolean> = _isProcessingReveal

    private fun createClearContext(currentProjects: List<Project>) =
        createClearExecutionContext(
            currentProjects = currentProjects,
            subStateStack = searchUseCase.subStateStack,
            searchUseCase = searchUseCase,
            planningUseCase = planningUseCase,
            enhancedNavigationManager = enhancedNavigationManager,
            uiEventChannel = uiEventChannel,
        )

    fun onNavigateToProject(scope: CoroutineScope, projectId: String) {
        if (_isProcessingReveal.value) return

        scope.launch {
            _isProcessingReveal.value = true
            try {
                val project = allProjectsFlat?.value?.find { it.id == projectId }
                val projectName = project?.name ?: "Unknown Project"

                val result =
                    clearAndNavigateHomeUseCase.execute(
                        command = ClearCommand.NavigateToProject(projectId, projectName),
                        context = createClearContext(allProjectsFlat?.value ?: emptyList()),
                    )

                if (result is ClearResult.Error) {
                    uiEventChannel?.send(
                        ProjectUiEvent.ShowToast("Помилка навігації до проєкту: ${result.message}")
                    )
                }
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }

    fun onCollapseAll(scope: CoroutineScope) {
        if (_isProcessingReveal.value) return

        scope.launch {
            _isProcessingReveal.value = true
            try {
                val result =
                    clearAndNavigateHomeUseCase.execute(
                        command = ClearCommand.CollapseAll,
                        context = createClearContext(allProjectsFlat?.value ?: emptyList()),
                    )

                when (result) {
                    is ClearResult.Success -> {
                        uiEventChannel?.send(ProjectUiEvent.ShowToast("Всі проєкти згорнуто"))
                    }
                    is ClearResult.Error -> {
                        uiEventChannel?.send(ProjectUiEvent.ShowToast("Помилка згортання: ${result.message}"))
                    }
                }
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }
}
