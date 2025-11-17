package com.romankozak.forwardappmobile.features.projectchooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.di.IoDispatcher

data class ProjectChooserUiState(
    val projects: List<Project> = emptyList(),
    val selectedProject: Project? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@Inject
class ProjectChooserViewModel(
    private val projectRepository: ProjectRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    lateinit var savedStateHandle: SavedStateHandle

    private val _uiState = MutableStateFlow(ProjectChooserUiState())
    val uiState = _uiState.asStateFlow()

    init {
        projectRepository.getAllProjects()
            .onEach { projects ->
                _uiState.update { it.copy(projects = projects, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onProjectSelected(project: Project) {
        _uiState.update { it.copy(selectedProject = project) }
        // TODO: Handle navigation back with result
    }
}
