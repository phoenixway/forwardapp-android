package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlobalSearchUiState(
    val results: List<GlobalSearchResult> = emptyList(),
    val isLoading: Boolean = true
)

class GlobalSearchViewModel(
    private val goalDao: GoalDao,
    val query: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    init {
        performSearch()
    }

    private fun performSearch() {
        viewModelScope.launch {
            val results = goalDao.searchGoalsGlobal("%$query%")
            _uiState.update {
                it.copy(results = results, isLoading = false)
            }
        }
    }
}