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
        // Запит не може бути пустим, але робимо перевірку
        if (query.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            // --- ВИПРАВЛЕНО: Використовуємо новий метод з GoalRepository ---
            val results = goalRepository.searchGoalsGlobal("%$query%")
            _uiState.update {
                it.copy(results = results, isLoading = false)
            }
        }
    }
}