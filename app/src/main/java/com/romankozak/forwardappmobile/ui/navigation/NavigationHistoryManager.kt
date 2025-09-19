package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Менеджер історії навігації для браузерної навігації назад/вперед
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

    // Стани
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _currentEntry = MutableStateFlow<NavigationEntry?>(null)
    val currentEntry: StateFlow<NavigationEntry?> = _currentEntry.asStateFlow()

    // Внутрішні змінні
    private var history: MutableList<NavigationEntry> = mutableListOf()
    private var currentIndex: Int = -1
    private var isNavigatingFromHistory = false

    init {
        loadHistoryFromSavedState()
    }

    /**
     * Додає новий запис до історії (викликається при переході на новий екран)
     */
    fun addEntry(entry: NavigationEntry) {
        if (isNavigatingFromHistory) {
            isNavigatingFromHistory = false
            return
        }

        Log.d(TAG, "Adding new entry: ${entry.type} - ${entry.title}")

        // Якщо ми не в кінці історії, видаляємо всі записи після поточного
        if (currentIndex < history.size - 1) {
            history = history.subList(0, currentIndex + 1)
        }

        // Додаємо новий запис
        history.add(entry)
        currentIndex = history.size - 1

        // Обмежуємо розмір історії
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
            currentIndex--
        }

        updateStates()
        saveHistoryToSavedState()
    }

    /**
     * Повертається назад в історії
     */
    fun goBack(): NavigationEntry? {
        if (!canGoBack.value) return null

        currentIndex--
        isNavigatingFromHistory = true

        val entry = history.getOrNull(currentIndex)
        Log.d(TAG, "Going back to: ${entry?.type} - ${entry?.title}")

        updateStates()
        saveHistoryToSavedState()
        return entry
    }

    /**
     * Йде вперед в історії
     */
    fun goForward(): NavigationEntry? {
        if (!canGoForward.value) return null

        currentIndex++
        isNavigatingFromHistory = true

        val entry = history.getOrNull(currentIndex)
        Log.d(TAG, "Going forward to: ${entry?.type} - ${entry?.title}")

        updateStates()
        saveHistoryToSavedState()
        return entry
    }

    /**
     * Отримує повну історію для відображення в UI
     */
    fun getFullHistory(): List<NavigationEntry> = history.toList()

    /**
     * Переходить до конкретного запису в історії
     */
    fun goToEntry(targetIndex: Int): NavigationEntry? {
        if (targetIndex !in history.indices) return null

        currentIndex = targetIndex
        isNavigatingFromHistory = true

        val entry = history[currentIndex]
        Log.d(TAG, "Going to history entry at index $targetIndex: ${entry.type} - ${entry.title}")

        updateStates()
        saveHistoryToSavedState()
        return entry
    }

    /**
     * Очищає всю історію
     */
    fun clearHistory() {
        history.clear()
        currentIndex = -1
        updateStates()
        saveHistoryToSavedState()
        Log.d(TAG, "History cleared")
    }

    /**
     * Оновлює поточний запис (наприклад, при зміні назви проекту)
     */
    fun updateCurrentEntry(updatedEntry: NavigationEntry) {
        if (currentIndex >= 0 && currentIndex < history.size) {
            history[currentIndex] = updatedEntry
            _currentEntry.value = updatedEntry
            saveHistoryToSavedState()
        }
    }

    private fun updateStates() {
        _canGoBack.value = currentIndex > 0
        _canGoForward.value = currentIndex < history.size - 1
        _currentEntry.value = history.getOrNull(currentIndex)

        Log.d(TAG, "Updated states: canGoBack=${_canGoBack.value}, canGoForward=${_canGoForward.value}, currentIndex=$currentIndex, historySize=${history.size}")
    }

    private fun loadHistoryFromSavedState() {
        val savedHistory = savedStateHandle.get<List<NavigationEntry>>(HISTORY_KEY)
        val savedIndex = savedStateHandle.get<Int>(CURRENT_INDEX_KEY) ?: -1

        if (!savedHistory.isNullOrEmpty()) {
            history = savedHistory.toMutableList()
            currentIndex = savedIndex.coerceIn(-1, history.size - 1)
            updateStates()
            Log.d(TAG, "Loaded history from saved state: ${history.size} entries, currentIndex=$currentIndex")
        }
    }

    private fun saveHistoryToSavedState() {
        scope.launch {
            savedStateHandle[HISTORY_KEY] = history.toList()
            savedStateHandle[CURRENT_INDEX_KEY] = currentIndex
        }
    }
}

/**
 * Запис в історії навігації
 */
data class NavigationEntry(
    val type: NavigationType,
    val id: String, // projectId для проектів, "main" для головного екрану
    val title: String, // назва проекту або "Проекти"
    val route: String, // повний route для навігації
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap() // додаткові дані
) {
    companion object {
        fun createMainScreen(): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.MAIN_SCREEN,
                id = "main",
                title = "Проекти",
                route = "goal_lists_screen"
            )
        }

        fun createProjectScreen(projectId: String, projectName: String): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.PROJECT_SCREEN,
                id = projectId,
                title = projectName,
                route = "goal_detail_screen/$projectId"
            )
        }

        fun createGlobalSearch(query: String): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.GLOBAL_SEARCH,
                id = "search_$query",
                title = "Пошук: $query",
                route = "global_search_screen/$query"
            )
        }
    }
}

enum class NavigationType {
    MAIN_SCREEN,
    PROJECT_SCREEN,
    GLOBAL_SEARCH,
    SETTINGS,
    EDIT_PROJECT
}