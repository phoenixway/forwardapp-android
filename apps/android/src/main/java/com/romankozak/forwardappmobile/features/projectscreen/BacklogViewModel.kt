package com.romankozak.forwardappmobile.features.projectscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectScreenEvent
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectScreenUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class BacklogViewModel(
    private val projectRepository: ProjectRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @Assisted private val projectId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectScreenUiState())
    val uiState: StateFlow<ProjectScreenUiState> = _uiState.asStateFlow()

    init {
        observeProject()
    }

    private fun observeProject() {
        if (projectId == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Project ID is null") }
            return
        }

        projectRepository
            .getProjectById(projectId)
            .onEach { project ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        errorMessage = null,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ProjectScreenEvent) {
        // TODO: Implement event handling
    }
}