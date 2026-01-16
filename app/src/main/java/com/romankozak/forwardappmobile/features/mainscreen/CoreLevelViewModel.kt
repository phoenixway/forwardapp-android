package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CoreLevelUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CoreLevelViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val uiState: StateFlow<CoreLevelUiState> =
        projectRepository.getAllProjectsFlow()
            .map { projects ->
                val coreProjects = projects.filter {
                    it.tags?.contains("main-beacons") == true || it.tags?.contains("core") == true
                }
                CoreLevelUiState(projects = coreProjects)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = CoreLevelUiState(isLoading = true)
            )
}