package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.NavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ДОДАНО: Клас для результатів навігації
data class NavigationResult(val key: String, val value: String)

// NEW: Клас для представлення навігаційних команд
sealed class NavigationCommand {
    data class Navigate(val route: String, val builder: NavOptionsBuilder.() -> Unit = {}) : NavigationCommand()
    data class PopBackStack(val key: String? = null, val value: String? = null) : NavigationCommand()
}

/**
 * Розширений менеджер навігації, ВІДВ'ЯЗАНИЙ від NavController.
 * Тепер він генерує навігаційні події.
 */
class EnhancedNavigationManager(
    savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "Nav_DEBUG"
    }

    private val historyManager = NavigationHistoryManager(savedStateHandle, scope)

    // NEW: Канал для надсилання навігаційних команд
    private val _navigationChannel = Channel<NavigationCommand>()
    val navigationCommandFlow = _navigationChannel.receiveAsFlow()

    // ДОДАНО: Канал для результатів навігації
    private val _navigationResults = MutableSharedFlow<NavigationResult>()
    val navigationResults: SharedFlow<NavigationResult> = _navigationResults.asSharedFlow()

    val canGoBack: StateFlow<Boolean> = historyManager.canGoBack
    val canGoForward: StateFlow<Boolean> = historyManager.canGoForward
    val currentEntry: StateFlow<NavigationEntry?> = historyManager.currentEntry

    private val _showNavigationMenu = MutableStateFlow(false)
    val showNavigationMenu: StateFlow<Boolean> = _showNavigationMenu.asStateFlow()

    fun navigateToMainScreen(isInitial: Boolean = false) {
        val entry = NavigationEntry.createMainScreen()
        historyManager.addEntry(entry)
        if (!isInitial) {
            // Замість виклику navController, надсилаємо команду
            sendNavigationCommand(NavigationCommand.Navigate("goal_lists_screen"))
        }
    }

    fun navigateToProject(projectId: String, projectName: String) {
        val entry = NavigationEntry.createProjectScreen(projectId, projectName)
        historyManager.addEntry(entry)
        sendNavigationCommand(NavigationCommand.Navigate("goal_detail_screen/$projectId"))
    }

    fun navigateToGlobalSearch(query: String) {
        val entry = NavigationEntry.createGlobalSearch(query)
        historyManager.addEntry(entry)
        sendNavigationCommand(NavigationCommand.Navigate("global_search_screen/$query"))
    }

    fun goBack() {
        val entry = historyManager.goBack()
        if (entry != null) {
            navigateToEntry(entry)
        } else {
            // Якщо в історії немає куди йти, надсилаємо команду повернутися назад по системі
            sendNavigationCommand(NavigationCommand.PopBackStack())
        }
    }

    fun goBackWithResult(key: String, value: String) {
        // ДОДАНО: Надсилаємо результат через flow
        scope.launch {
            _navigationResults.emit(NavigationResult(key, value))
        }

        // Потім виконуємо навігацію назад
        sendNavigationCommand(NavigationCommand.PopBackStack(key, value))
        // Оновлюємо внутрішній стан історії, ніби ми повернулись назад
        historyManager.goBack()
    }

    fun goForward() {
        val entry = historyManager.goForward()
        if (entry != null) {
            navigateToEntry(entry)
        }
    }

    fun showNavigationMenu() {
        _showNavigationMenu.value = true
    }

    fun hideNavigationMenu() {
        _showNavigationMenu.value = false
    }

    fun navigateToHistoryEntry(index: Int) {
        val entry = historyManager.goToEntry(index)
        if (entry != null) {
            navigateToEntry(entry)
        }
        hideNavigationMenu()
    }

    fun getNavigationHistory(): List<NavigationEntry> {
        return historyManager.getFullHistory()
    }

    fun updateCurrentEntry(updatedTitle: String) {
        val current = currentEntry.value
        if (current != null) {
            val updated = current.copy(title = updatedTitle)
            historyManager.updateCurrentEntry(updated)
        }
    }

    // ДОДАНО: Метод для відправки результату без навігації
    fun sendResult(key: String, value: String) {
        scope.launch {
            _navigationResults.emit(NavigationResult(key, value))
        }
    }

    // ДОДАНО: Метод для очищення історії
    fun clearHistory() {
        historyManager.clearHistory()
    }

    // ДОДАНО: Поліпшена навігація до домашнього стану
    fun navigateHome() {
        // Очищуємо всю історію та повертаємося до основного екрану
        historyManager.clearHistory()

        // Додаємо новий запис для основного екрану
        val homeEntry = NavigationEntry.createMainScreen()
        historyManager.addEntry(homeEntry)

        // Навігуємо до основного екрану
        sendNavigationCommand(NavigationCommand.Navigate("goal_lists_screen") {
            popUpTo("goal_lists_screen") { inclusive = true }
            launchSingleTop = true
        })
    }

    private fun navigateToEntry(entry: NavigationEntry) {
        Log.d(TAG, "Navigating to history entry: ${entry.type} - ${entry.title}")

        val command = when (entry.type) {
            NavigationType.MAIN_SCREEN -> NavigationCommand.Navigate("goal_lists_screen") {
                // Очищуємо стек до головного екрану, щоб уникнути циклів
                popUpTo("goal_lists_screen") { inclusive = false }
            }
            NavigationType.PROJECT_SCREEN -> NavigationCommand.Navigate("goal_detail_screen/${entry.id}")
            NavigationType.GLOBAL_SEARCH -> {
                val query = entry.id.removePrefix("search_")
                NavigationCommand.Navigate("global_search_screen/$query")
            }
            else -> NavigationCommand.Navigate(entry.route)
        }
        sendNavigationCommand(command)
    }

    private fun sendNavigationCommand(command: NavigationCommand) {
        scope.launch {
            _navigationChannel.send(command)
        }
    }

    fun navigateHomeWithResult(key: String, value: String) {
        // 1. Надсилаємо результат, як це робить goBackWithResult
        scope.launch {
            _navigationResults.emit(NavigationResult(key, value))
        }

        // 2. Виконуємо логіку навігації на головний екран з navigateHome
        historyManager.clearHistory()
        val homeEntry = NavigationEntry.createMainScreen()
        historyManager.addEntry(homeEntry)
        sendNavigationCommand(NavigationCommand.Navigate("goal_lists_screen") {
            popUpTo("goal_lists_screen") { inclusive = true }
            launchSingleTop = true
        })
    }

}
