package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry // <-- CORRECT IMPORT ADDED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the browser-like navigation history (back/forward).
 */
class NavigationHistoryManager(
    private val savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "NavigationHistory"
        private const val HISTORY_KEY = "navigation_history"
        private const val CURRENT_INDEX_KEY = "current_history_index"
        private const val MAX_HISTORY_SIZE = 50
    }

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _currentEntry = MutableStateFlow<NavigationEntry?>(null)
    val currentEntry: StateFlow<NavigationEntry?> = _currentEntry.asStateFlow()

    private var history: MutableList<NavigationEntry> = mutableListOf()
    private var currentIndex: Int = -1
    private var isNavigatingFromHistory = false

    init {
        loadHistoryFromSavedState()
    }

    fun addEntry(entry: NavigationEntry) {
        if (isNavigatingFromHistory) {
            isNavigatingFromHistory = false
            return
        }

        if (currentIndex < history.size - 1) {
            history = history.subList(0, currentIndex + 1)
        }

        // Avoid adding duplicate consecutive entries
        if (history.lastOrNull()?.route == entry.route) {
            return
        }

        history.add(entry)
        currentIndex = history.size - 1

        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
            currentIndex--
        }

        updateStates()
        saveHistoryToSavedState()
    }

    fun goBack(): NavigationEntry? {
        if (!canGoBack.value) return null

        currentIndex--
        isNavigatingFromHistory = true

        val entry = history.getOrNull(currentIndex)
        updateStates()
        saveHistoryToSavedState()
        return entry
    }

    fun goForward(): NavigationEntry? {
        if (!canGoForward.value) return null

        currentIndex++
        isNavigatingFromHistory = true

        val entry = history.getOrNull(currentIndex)
        updateStates()
        saveHistoryToSavedState()
        return entry
    }

    fun getFullHistory(): List<NavigationEntry> = history.toList()

    fun goToEntry(targetIndex: Int): NavigationEntry? {
        if (targetIndex !in history.indices) return null

        currentIndex = targetIndex
        isNavigatingFromHistory = true

        val entry = history[currentIndex]
        updateStates()
        saveHistoryToSavedState()
        return entry
    }

    fun clearHistory() {
        history.clear()
        currentIndex = -1
        updateStates()
        saveHistoryToSavedState()
    }

    fun updateCurrentEntry(updatedEntry: NavigationEntry) {
        if (currentIndex in history.indices) {
            history[currentIndex] = updatedEntry
            _currentEntry.value = updatedEntry
            saveHistoryToSavedState()
        }
    }

    private fun updateStates() {
        _canGoBack.value = currentIndex > 0
        _canGoForward.value = currentIndex < history.size - 1
        _currentEntry.value = history.getOrNull(currentIndex)
    }

    private fun loadHistoryFromSavedState() {
        val savedHistory = savedStateHandle.get<List<NavigationEntry>>(HISTORY_KEY)
        val savedIndex = savedStateHandle.get<Int>(CURRENT_INDEX_KEY) ?: -1

        if (!savedHistory.isNullOrEmpty()) {
            history = savedHistory.toMutableList()
            currentIndex = savedIndex.coerceIn(-1, history.size - 1)
            updateStates()
        }
    }

    private fun saveHistoryToSavedState() {
        scope.launch {
            savedStateHandle[HISTORY_KEY] = history.toList()
            savedStateHandle[CURRENT_INDEX_KEY] = currentIndex
        }
    }
}