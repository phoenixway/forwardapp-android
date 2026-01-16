package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.features.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.features.navigation.ClearCommand
import com.romankozak.forwardappmobile.features.navigation.ClearResult
import com.romankozak.forwardappmobile.features.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.navigation.createClearExecutionContext
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
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
    private var enhancedNavigationManager: EnhancedNavigationManager? = null
    private var uiEventChannel: Channel<ProjectUiEvent>? = null
    private var allProjectsFlat: StateFlow<List<Project>>? = null

    private val _isProcessingReveal = MutableStateFlow(false)
    val isProcessingReveal: StateFlow<Boolean> = _isProcessingReveal

    private val _isAttached = MutableStateFlow(false)
    val isAttached: StateFlow<Boolean> = _isAttached

    fun attach(
        enhancedNavigationManager: EnhancedNavigationManager,
        uiEventChannel: Channel<ProjectUiEvent>,
        allProjectsFlat: StateFlow<List<Project>>,
    ) {
        this.enhancedNavigationManager = enhancedNavigationManager
        this.uiEventChannel = uiEventChannel
        this.allProjectsFlat = allProjectsFlat
        _isAttached.value = true
    }

    fun detach() {
        enhancedNavigationManager = null
        uiEventChannel = null
        allProjectsFlat = null
        _isProcessingReveal.value = false
        _isAttached.value = false
    }

    private fun createClearContext(currentProjects: List<Project>) =
        enhancedNavigationManager?.let { manager ->
            uiEventChannel?.let { channel ->
                createClearExecutionContext(
                    currentProjects = currentProjects,
                    subStateStack = searchUseCase.subStateStack,
                    searchUseCase = searchUseCase,
                    planningUseCase = planningUseCase,
                    enhancedNavigationManager = manager,
                    uiEventChannel = channel,
                )
            }
        }

    fun onNavigateHome(scope: CoroutineScope) {
        if (!_isAttached.value || _isProcessingReveal.value) return

        scope.launch {
            val context =
                createClearContext(allProjectsFlat?.value ?: emptyList()) ?: return@launch
            val channel = uiEventChannel ?: return@launch
            _isProcessingReveal.value = true
            try {
                val result =
                    clearAndNavigateHomeUseCase.execute(
                        command = ClearCommand.Home,
                        context = context,
                    )

                if (result is ClearResult.Error) {
                    channel.send(
                        ProjectUiEvent.ShowToast("Помилка навігації: ${result.message}")
                    )
                }
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }

    fun onNavigateToProject(scope: CoroutineScope, projectId: String) {
        if (!_isAttached.value || _isProcessingReveal.value) return

        scope.launch {
            val context =
                createClearContext(allProjectsFlat?.value ?: emptyList()) ?: return@launch
            val channel = uiEventChannel ?: return@launch
            _isProcessingReveal.value = true
            try {
                val project = allProjectsFlat?.value?.find { it.id == projectId }
                val projectName = project?.name ?: "Unknown Project"

                val result =
                    clearAndNavigateHomeUseCase.execute(
                        command = ClearCommand.NavigateToProject(projectId, projectName),
                        context = context,
                    )

                if (result is ClearResult.Error) {
                    channel.send(
                        ProjectUiEvent.ShowToast("Помилка навігації до проєкту: ${result.message}")
                    )
                }
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }

    fun onCollapseAll(scope: CoroutineScope) {
        if (!_isAttached.value || _isProcessingReveal.value) return

        scope.launch {
            val context =
                createClearContext(allProjectsFlat?.value ?: emptyList()) ?: return@launch
            val channel = uiEventChannel ?: return@launch
            _isProcessingReveal.value = true
            try {
                val result =
                    clearAndNavigateHomeUseCase.execute(
                        command = ClearCommand.CollapseAll,
                        context = context,
                    )

                when (result) {
                    is ClearResult.Success -> {
                        channel.send(ProjectUiEvent.ShowToast("Всі проєкти згорнуто"))
                    }
                    is ClearResult.Error -> {
                        channel.send(ProjectUiEvent.ShowToast("Помилка згортання: ${result.message}"))
                    }
                }
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }
}
