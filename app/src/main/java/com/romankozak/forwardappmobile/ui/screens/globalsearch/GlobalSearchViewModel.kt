package com.romankozak.forwardappmobile.ui.screens.globalsearch

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
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
    val query: String = savedStateHandle.get<String>("query") ?: ""

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    val obsidianVaultName: StateFlow<String> =
        settingsRepository.obsidianVaultNameFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        performSearch()
    }

    private fun performSearch() {
        if (query.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            val results = projectRepository.searchGlobal("%$query%")

            val sublistItems = results.filterIsInstance<GlobalSearchResultItem.SublistItem>()
            Log.d(
                "PATH_DEBUG",
                "[VIEWMODEL] Всього результатів: ${results.size}. З них підпроектів: ${sublistItems.size}"
            )
            sublistItems.firstOrNull()?.let {
                Log.d(
                    "PATH_DEBUG",
                    "[VIEWMODEL] Перший підпроект: name='${it.searchResult.subproject.name}', pathSegments=${it.searchResult.pathSegments}"
                )
            }

            val distinctResults = results.distinctBy { it.uniqueId }
            _uiState.update {
                it.copy(results = distinctResults, isLoading = false)
            }
        }
    }
}