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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchHistory: List<String> = emptyList(),
)

@HiltViewModel
class GlobalSearchViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        companion object {
            private const val SEARCH_HISTORY_KEY = "global_search_history"
            private const val MAX_SEARCH_HISTORY = 12
        }

        private val initialQueryRaw: String = savedStateHandle["query"] ?: ""
        private val initialQuery: String = runCatching { URLDecoder.decode(initialQueryRaw, "UTF-8") }.getOrDefault(initialQueryRaw)
        private val _uiState = MutableStateFlow(GlobalSearchUiState())
        val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()
        private var searchJob: Job? = null

        lateinit var enhancedNavigationManager: EnhancedNavigationManager

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        init {
            val history = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
            _uiState.update { it.copy(query = initialQuery, searchHistory = history) }
            if (initialQuery.isNotBlank()) {
                performSearch(initialQuery)
            }
        }

        fun goBackToRevealProject(projectId: String) {
            enhancedNavigationManager.goBackWithResult("project_to_reveal", projectId)
        }

        fun onQueryChange(value: String) {
            _uiState.update { it.copy(query = value) }
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    delay(280)
                    performSearch(value)
                }
        }

        fun onSubmitSearch() {
            val query = _uiState.value.query
            addSearchQueryToHistory(query)
            performSearch(query)
        }

        fun onSelectHistoryQuery(query: String) {
            _uiState.update { it.copy(query = query) }
            addSearchQueryToHistory(query)
            performSearch(query)
        }

        fun removeSearchHistoryEntry(query: String) {
            if (query.isBlank()) return
            val updated = _uiState.value.searchHistory.filterNot { it.equals(query, ignoreCase = true) }
            _uiState.update { it.copy(searchHistory = updated) }
            savedStateHandle[SEARCH_HISTORY_KEY] = updated
        }

        fun clearSearchHistory() {
            _uiState.update { it.copy(searchHistory = emptyList()) }
            savedStateHandle[SEARCH_HISTORY_KEY] = emptyList<String>()
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

        private fun addSearchQueryToHistory(rawQuery: String) {
            val query = rawQuery.trim()
            if (query.isBlank()) return

            val currentHistory = _uiState.value.searchHistory
            val withoutCurrent = currentHistory.filterNot { it.equals(query, ignoreCase = true) }
            val updated = (listOf(query) + withoutCurrent).take(MAX_SEARCH_HISTORY)

            _uiState.update { it.copy(searchHistory = updated) }
            savedStateHandle[SEARCH_HISTORY_KEY] = updated
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
