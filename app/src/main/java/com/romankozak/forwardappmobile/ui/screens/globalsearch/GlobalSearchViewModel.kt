package com.romankozak.forwardappmobile.ui.screens.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalSearchUiState(
    val results: List<GlobalSearchResult> = emptyList(),
    val isLoading: Boolean = true
)

// --- ВИПРАВЛЕНО: Додаємо анотації для Hilt ---
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    // --- ВИПРАВЛЕНО: Використовуємо GoalRepository ---
    private val goalRepository: GoalRepository,
    // --- ВИПРАВЛЕНО: Отримуємо query через SavedStateHandle ---
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ВИПРАВЛЕНО: Отримуємо query з аргументів навігації ---
    val query: String = savedStateHandle.get<String>("query") ?: ""

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    init {
        performSearch()
    }

    private fun performSearch() {
        // The query cannot be empty, but we check just in case
        if (query.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            // Fetch results from the repository
            val results = goalRepository.searchGoalsGlobal("%$query%")

            // FIX: Remove duplicate search results before they are passed to the UI.
            // The .distinct() call ensures every item in the list is unique.
            val distinctResults = results.distinct()

            _uiState.update {
                // Update the state with the sanitized, distinct list
                it.copy(results = distinctResults, isLoading = false)
            }
        }
    }
}