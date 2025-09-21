// Фінальна виправлена версія: ui/navigation/NavigationHistoryManager.kt

package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NavigationHistoryManager(
    private val savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope
) {
    companion object {
        private const val HISTORY_KEY = "navigation_history"
        private const val INDEX_KEY = "navigation_index"
        private const val TAG = "HistoryManager_DEBUG"
    }

    private val _history = MutableStateFlow<List<NavigationEntry>>(emptyList())
    private val _currentIndex = MutableStateFlow(-1)

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _currentEntry = MutableStateFlow<NavigationEntry?>(null)
    val currentEntry: StateFlow<NavigationEntry?> = _currentEntry.asStateFlow()

    init {
        var savedHistory = savedStateHandle.get<List<NavigationEntry>>(HISTORY_KEY) ?: emptyList()
        var savedIndex = savedStateHandle.get<Int>(INDEX_KEY) ?: -1

        // ✅ ГОЛОВНЕ ВИПРАВЛЕННЯ:
        // Якщо історія порожня, примусово додаємо головний екран як базу.
        if (savedHistory.isEmpty()) {
            savedHistory = listOf(NavigationEntry.createMainScreen())
            savedIndex = 0
            Log.w(TAG, "INIT: Історія була порожньою. Створено базовий запис для головного екрана.")
        }

        _history.value = savedHistory
        _currentIndex.value = savedIndex
        Log.i(TAG, "INIT: Історію відновлено: ${savedHistory.size} елементів, індекс: $savedIndex")
        updateStates("INIT")
    }

    private fun saveState() {
        scope.launch {
            savedStateHandle[HISTORY_KEY] = _history.value
            savedStateHandle[INDEX_KEY] = _currentIndex.value
        }
    }

    // ... (решта файлу залишається без змін, як у попередній версії)

    private fun updateStates(caller: String) {
        val history = _history.value
        val index = _currentIndex.value

        val previousCanGoBack = _canGoBack.value
        val newCanGoBack = index > 0

        _canGoBack.value = newCanGoBack
        _canGoForward.value = index < history.size - 1
        _currentEntry.value = history.getOrNull(index)

        if (previousCanGoBack != newCanGoBack) {
            Log.w(TAG, "[$caller] <<< ЗМІНА СТАНУ canGoBack: з $previousCanGoBack на $newCanGoBack >>>")
        }
        Log.i(TAG, "[$caller] updateStates: index=$index, historySize=${history.size}. Розраховано canGoBack = $newCanGoBack")

        saveState()
    }

    fun addEntry(entry: NavigationEntry) {
        Log.d(TAG, "[addEntry] Спроба додати: ${entry.title}")
        _history.update { currentHistory ->
            val index = _currentIndex.value

            val relevantHistory = if (index < currentHistory.size - 1) {
                Log.d(TAG, "[addEntry] Обрізаємо майбутню історію. Індекс $index, розмір ${currentHistory.size}")
                currentHistory.subList(0, index + 1).toList()
            } else {
                currentHistory
            }

            if (relevantHistory.lastOrNull()?.route == entry.route) {
                Log.d(TAG, "[addEntry] Ігноруємо дублікат.")
                relevantHistory
            } else {
                relevantHistory + entry
            }
        }

        _currentIndex.value = _history.value.size - 1
        updateStates("addEntry")
    }

    fun goBack(): NavigationEntry? {
        Log.d(TAG, "[goBack] Викликано. Поточний індекс: ${_currentIndex.value}, canGoBack: ${_canGoBack.value}")
        if (_canGoBack.value) {
            _currentIndex.value--
            Log.d(TAG, "[goBack] Індекс зменшено до ${_currentIndex.value}")
            updateStates("goBack")
            return _currentEntry.value
        }
        Log.w(TAG, "[goBack] Повертатися далі неможливо.")
        return null
    }

    fun goForward(): NavigationEntry? {
        if (_canGoForward.value) {
            _currentIndex.value++
            updateStates("goForward")
            return _currentEntry.value
        }
        return null
    }

    fun goToEntry(index: Int): NavigationEntry? {
        if (index in _history.value.indices) {
            _currentIndex.value = index
            updateStates("goToEntry")
            return _currentEntry.value
        }
        return null
    }

    fun updateCurrentEntry(updatedEntry: NavigationEntry) {
        _history.update { currentHistory ->
            val index = _currentIndex.value
            if (index in currentHistory.indices) {
                currentHistory.toMutableList().apply { this[index] = updatedEntry }
            } else {
                currentHistory
            }
        }
        updateStates("updateCurrentEntry")
    }

    fun getFullHistory(): List<NavigationEntry> = _history.value

    fun clearHistory() {
        Log.w(TAG, "[clearHistory] Історію повністю очищено.")
        _history.value = listOf(NavigationEntry.createMainScreen())
        _currentIndex.value = 0
        updateStates("clearHistory")
    }
}