package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class GlobalSearchViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val initialQueryRaw: String = savedStateHandle["query"] ?: ""
        private val initialQuery: String = runCatching { URLDecoder.decode(initialQueryRaw, "UTF-8") }.getOrDefault(initialQueryRaw)
        private val _uiState = MutableStateFlow(GlobalSearchUiState())
        val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

        lateinit var enhancedNavigationManager: EnhancedNavigationManager

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        init {
            _uiState.update { it.copy(query = initialQuery) }
            if (initialQuery.isNotBlank()) {
                performSearch(initialQuery)
            }
        }

        fun goBackToRevealProject(projectId: String) {
            enhancedNavigationManager.goBackWithResult("project_to_reveal", projectId)
        }

        fun onQueryChange(value: String) {
            _uiState.update { it.copy(query = value) }
        }

        fun onSubmitSearch() {
            performSearch(_uiState.value.query)
        }

        private fun performSearch(rawQuery: String) {
            val query = rawQuery.trim()
            if (query.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                return
            }

            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val results = contextRepository.searchGlobal("%$query%")
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
                val finalProjectName = projectName ?: contextRepository.getContextById(projectId)?.name ?: "Context"
                enhancedNavigationManager.navigateToProject(projectId, finalProjectName)
            }
        }
    }
