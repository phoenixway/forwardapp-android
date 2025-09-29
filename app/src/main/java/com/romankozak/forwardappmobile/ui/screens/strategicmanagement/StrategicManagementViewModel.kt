
package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StrategicManagementViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrategicManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val _currentTab = MutableStateFlow(StrategicManagementTab.DASHBOARD)
    val currentTab = _currentTab.asStateFlow()

    init {
        loadDashboardProjects()
        loadElementsProjects()
    }

    fun onTabSelected(tab: StrategicManagementTab) {
        _currentTab.value = tab
    }

    private fun loadDashboardProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val longTermIds = projectRepository.findProjectIdsByTag("long-term-strategy")
                val strReviewIds = projectRepository.findProjectIdsByTag("str-review")
                val middleTermIds = projectRepository.findProjectIdsByTag("middle-term-backlog")
                val activeQuestsIds = projectRepository.findProjectIdsByTag("active-quests")
                val projectIds = (longTermIds + strReviewIds + middleTermIds + activeQuestsIds).distinct()
                val allProjects = projectRepository.getAllProjects()
                val projects = projectIds.mapNotNull { id -> allProjects.find { it.id == id } }
                _uiState.update { it.copy(dashboardProjects = projects, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadElementsProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allProjects = projectRepository.getAllProjects()
                val mediumIds = projectRepository.findProjectIdsByTag("medium")
                val longIds = projectRepository.findProjectIdsByTag("long")
                val strIds = projectRepository.findProjectIdsByTag("str")
                val rootProjectIds = (mediumIds + longIds + strIds).distinct()

                val elementsProjects = mutableListOf<Project>()
                val projectsToAdd = rootProjectIds.mapNotNull { projectId -> allProjects.find { it.id == projectId } }.toMutableList()
                val processedProjects = mutableSetOf<String>()

                while (projectsToAdd.isNotEmpty()) {
                    val currentProject = projectsToAdd.removeAt(0)
                    if (processedProjects.add(currentProject.id)) {
                        elementsProjects.add(currentProject)
                        val children = allProjects.filter { it.parentId == currentProject.id }
                        projectsToAdd.addAll(children)
                    }
                }

                _uiState.update { it.copy(elementsProjects = elementsProjects.distinctBy { it.id }, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
