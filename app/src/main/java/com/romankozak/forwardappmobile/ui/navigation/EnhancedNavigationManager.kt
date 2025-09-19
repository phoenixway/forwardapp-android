package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Розширений менеджер навігації з підтримкою історії
 */
class EnhancedNavigationManager(
    private val navController: NavController,
    savedStateHandle: SavedStateHandle,
    scope: CoroutineScope
) {
    companion object {
        private const val TAG = "EnhancedNavigation"
    }

    private val historyManager = NavigationHistoryManager(savedStateHandle, scope)

    // Делегуємо StateFlow з historyManager
    val canGoBack: StateFlow<Boolean> = historyManager.canGoBack
    val canGoForward: StateFlow<Boolean> = historyManager.canGoForward
    val currentEntry: StateFlow<NavigationEntry?> = historyManager.currentEntry

    private val _showNavigationMenu = MutableStateFlow(false)
    val showNavigationMenu: StateFlow<Boolean> = _showNavigationMenu.asStateFlow()

    fun navigateToMainScreen(isInitial: Boolean = false) {
        val entry = NavigationEntry.createMainScreen()
        historyManager.addEntry(entry)

        // Only navigate if this isn't the initial screen setup,
        // as NavHost already handles displaying the start destination.
        if (!isInitial) {
            navController.navigate("goal_lists_screen")
        }
        Log.d(TAG, "Navigated to main screen (isInitial: $isInitial)")
    }

    /**
     * Навігація до конкретного проекту з додаванням до історії
     */
    fun navigateToProject(projectId: String, projectName: String) {
        val entry = NavigationEntry.createProjectScreen(projectId, projectName)
        historyManager.addEntry(entry)
        navController.navigate("goal_detail_screen/$projectId")
        Log.d(TAG, "Navigated to project: $projectName")
    }

    /**
     * Навігація до глобального пошуку
     */
    fun navigateToGlobalSearch(query: String) {
        val entry = NavigationEntry.createGlobalSearch(query)
        historyManager.addEntry(entry)
        navController.navigate("global_search_screen/$query")
        Log.d(TAG, "Navigated to global search: $query")
    }

    /**
     * Навігація назад через історію
     */
    fun goBack(): Boolean {
        val entry = historyManager.goBack()
        return if (entry != null) {
            navigateToEntry(entry)
            true
        } else {
            // Якщо в історії немає записів, використовуємо стандартну навігацію
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
                true
            } else {
                false
            }
        }
    }

    /**
     * Навігація вперед через історію
     */
    fun goForward(): Boolean {
        val entry = historyManager.goForward()
        return if (entry != null) {
            navigateToEntry(entry)
            true
        } else {
            false
        }
    }

    /**
     * Показати меню історії навігації
     */
    fun showNavigationMenu() {
        _showNavigationMenu.value = true
    }

    /**
     * Сховати меню історії навігації
     */
    fun hideNavigationMenu() {
        _showNavigationMenu.value = false
    }

    /**
     * Перехід до конкретного запису в історії
     */
    fun navigateToHistoryEntry(index: Int) {
        val entry = historyManager.goToEntry(index)
        if (entry != null) {
            navigateToEntry(entry)
        }
        hideNavigationMenu()
    }

    /**
     * Отримати повну історію для відображення в UI
     */
    fun getNavigationHistory(): List<NavigationEntry> {
        return historyManager.getFullHistory()
    }

    /**
     * Оновити поточний запис (наприклад, при зміні назви проекту)
     */
    fun updateCurrentEntry(updatedTitle: String) {
        val current = currentEntry.value
        if (current != null) {
            val updated = current.copy(title = updatedTitle)
            historyManager.updateCurrentEntry(updated)
        }
    }

    /**
     * Очистити історію навігації
     */
    fun clearHistory() {
        historyManager.clearHistory()
    }

    /**
     * Внутрішній метод для навігації до запису
     */
    private fun navigateToEntry(entry: NavigationEntry) {
        Log.d(TAG, "Navigating to history entry: ${entry.type} - ${entry.title}")

        when (entry.type) {
            NavigationType.MAIN_SCREEN -> {
                navController.navigate("goal_lists_screen") {
                    // Очищаємо стек до головного екрану
                    popUpTo("goal_lists_screen") {
                        inclusive = false
                    }
                }
            }
            NavigationType.PROJECT_SCREEN -> {
                navController.navigate("goal_detail_screen/${entry.id}")
            }
            NavigationType.GLOBAL_SEARCH -> {
                val query = entry.id.removePrefix("search_")
                navController.navigate("global_search_screen/$query")
            }
            else -> {
                navController.navigate(entry.route)
            }
        }
    }

    /**
     * Метод для ініціалізації історії при першому запуску
     */
    fun initializeWithCurrentScreen(currentRoute: String?) {
        if (historyManager.getFullHistory().isEmpty()) {
            when {
                currentRoute?.contains("goal_lists_screen") == true -> {
                    historyManager.addEntry(NavigationEntry.createMainScreen())
                }
                currentRoute?.contains("goal_detail_screen") == true -> {
                    // Додамо головний екран, потім поточний проект
                    historyManager.addEntry(NavigationEntry.createMainScreen())
                    // Тут потрібно буде отримати назву проекту
                }
            }
        }
    }
}

/**
 * Composable для відображення меню історії навігації
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun NavigationHistoryMenu(
    navManager: EnhancedNavigationManager,
    onDismiss: () -> Unit
) {
    val history = remember { navManager.getNavigationHistory() }
    val currentEntry by navManager.currentEntry.collectAsState()

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Історія навігації",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = androidx.compose.ui.Modifier.padding(bottom = 16.dp)
            )

            androidx.compose.foundation.lazy.LazyColumn {
                itemsIndexed(history.reversed()) { reverseIndex, entry ->
                    val actualIndex = history.size - 1 - reverseIndex
                    val isCurrentEntry = entry == currentEntry
                    androidx.compose.material3.ListItem(
                        headlineContent = {
                            androidx.compose.material3.Text(entry.title)
                        },
                        supportingContent = {
                            androidx.compose.material3.Text(
                                when (entry.type) {
                                    NavigationType.MAIN_SCREEN -> "Головний екран"
                                    NavigationType.PROJECT_SCREEN -> "Проект"
                                    NavigationType.GLOBAL_SEARCH -> "Пошук"
                                    else -> entry.type.name
                                }
                            )
                        },
                        leadingContent = {
                            androidx.compose.material3.Icon(
                                imageVector = when (entry.type) {
                                    NavigationType.MAIN_SCREEN -> androidx.compose.material.icons.Icons.Outlined.Home
                                    NavigationType.PROJECT_SCREEN -> androidx.compose.material.icons.Icons.Outlined.Folder
                                    NavigationType.GLOBAL_SEARCH -> androidx.compose.material.icons.Icons.Outlined.Search
                                    else -> androidx.compose.material.icons.Icons.Outlined.Info
                                },
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            if (isCurrentEntry) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.RadioButtonChecked,
                                    contentDescription = "Поточна сторінка",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = androidx.compose.ui.Modifier
                            .clickable(enabled = !isCurrentEntry) {
                                navManager.navigateToHistoryEntry(actualIndex)
                            }
                            .alpha(if (isCurrentEntry) 0.6f else 1f)
                    )
                }
            }
        }
    }
}