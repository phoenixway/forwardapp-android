package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val projectRepository: ProjectRepositoryCore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    init {
        projectRepository.getAllProjectsFlow()
            .onEach { projects ->
                val hierarchy = ListHierarchyData(
                    topLevelProjects = projects.filter { it.parentId == null },
                    childMap = projects.filter { it.parentId != null }.groupBy { it.parentId!! },
                    allProjects = projects
                )
                _uiState.update { it.copy(projectHierarchy = hierarchy, isReadyForFiltering = true) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MainScreenEvent) {
        // Handle events if necessary
    }
}