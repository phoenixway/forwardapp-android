package com.romankozak.forwardappmobile.ui.screens.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalSearchUiState(
    val results: List<GlobalSearchResultItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class GlobalSearchViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
        private val settingsRepository: SettingsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val query: String = savedStateHandle["query"] ?: ""
        private val _uiState = MutableStateFlow(GlobalSearchUiState())
        val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

        lateinit var enhancedNavigationManager: EnhancedNavigationManager

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        init {
            performSearch()
        }

        
        fun goBackToRevealProject(projectId: String) {
            enhancedNavigationManager.goBackWithResult("project_to_reveal", projectId)
        }
        

        private fun performSearch() {
            if (query.isBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return
            }

            viewModelScope.launch {
                val results = projectRepository.searchGlobal("%$query%")
                val distinctResults = results.distinctBy { it.uniqueId }
                _uiState.update {
                    it.copy(results = distinctResults, isLoading = false)
                }
            }
        }

        fun navigateToProjectForResult(
            projectId: String,
            projectName: String?,
        ) {
            viewModelScope.launch {
                val finalProjectName = projectName ?: projectRepository.getProjectById(projectId)?.name ?: "Project"
                enhancedNavigationManager.navigateToProject(projectId, finalProjectName)
            }
        }
    }
