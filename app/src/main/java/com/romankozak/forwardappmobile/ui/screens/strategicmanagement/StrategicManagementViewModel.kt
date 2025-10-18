package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        loadData()
    }

    fun onTabSelected(tab: StrategicManagementTab) {
        _currentTab.value = tab
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                coroutineScope {
                    val dashboardJob = async { loadDashboardProjects() }
                    val elementsJob = async { loadElementsProjects() }
                    dashboardJob.await()
                    elementsJob.await()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadDashboardProjects() {
        val longTermIds = projectRepository.findProjectIdsByTag("long-term-strategy")
        val middleTermIds = projectRepository.findProjectIdsByTag("middle-term-backlog")
        val activeQuestsIds = projectRepository.findProjectIdsByTag("active-quests")
        val missionIds = projectRepository.findProjectIdsByTag("mission")
        val projectIds = (longTermIds + middleTermIds + activeQuestsIds + missionIds).distinct()

        val strReviewIds = projectRepository.findProjectIdsByTag("strategic-review")
        val strReviewProjectIds = strReviewIds.distinct()

        val allProjects = projectRepository.getAllProjects()

        val projects = projectIds.mapNotNull { id -> allProjects.find { it.id == id } }
        val strReviewProjects = strReviewProjectIds.mapNotNull { id -> allProjects.find { it.id == id } }

        _uiState.update { it.copy(dashboardProjects = projects + strReviewProjects) }
    }

    private suspend fun loadElementsProjects() {
        val allProjects = projectRepository.getAllProjects()
        val mediumIds = projectRepository.findProjectIdsByTag("medium")
        val longIds = projectRepository.findProjectIdsByTag("long")
        val strIds = projectRepository.findProjectIdsByTag("str")
        val rootProjectIds = (mediumIds + longIds + strIds).distinct()

        val elementsProjects = getProjectsWithChildren(rootProjectIds, allProjects)

        _uiState.update { it.copy(elementsProjects = elementsProjects) }
    }

    private fun getProjectsWithChildren(rootProjectIds: List<String>, allProjects: List<Project>): List<Project> {
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
        return elementsProjects.distinctBy { it.id }
    }
}