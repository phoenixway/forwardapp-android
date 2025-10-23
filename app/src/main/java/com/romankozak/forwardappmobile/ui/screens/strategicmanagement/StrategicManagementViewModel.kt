package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.strategicmanagement.usecases.GetStrategicProjectsUseCase
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
    private val getStrategicProjects: GetStrategicProjectsUseCase
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
                loadDashboardProjects()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadDashboardProjects() {
        val strategicProjects = getStrategicProjects()
        _uiState.update { it.copy(dashboardProjects = strategicProjects) }
    }


}