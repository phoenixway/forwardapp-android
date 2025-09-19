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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// --- NEW: Клас для представлення навігаційних команд ---
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

    // --- NEW: Канал для надсилання навігаційних команд ---
    private val _navigationChannel = Channel<NavigationCommand>()
    val navigationCommandFlow = _navigationChannel.receiveAsFlow()

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
}


// Composable для меню залишається тут, він не змінився
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationHistoryMenu(
    navManager: EnhancedNavigationManager,
    onDismiss: () -> Unit
) {
    // ... тіло функції без змін
    val history = remember { navManager.getNavigationHistory() }
    val currentEntry by navManager.currentEntry.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Історія навігації",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                itemsIndexed(history.reversed()) { reverseIndex, entry ->
                    val actualIndex = history.size - 1 - reverseIndex
                    val isCurrentEntry = entry == currentEntry

                    ListItem(
                        headlineContent = {
                            Text(entry.title)
                        },
                        supportingContent = {
                            Text(
                                when (entry.type) {
                                    NavigationType.MAIN_SCREEN -> "Головний екран"
                                    NavigationType.PROJECT_SCREEN -> "Проект"
                                    NavigationType.GLOBAL_SEARCH -> "Пошук"
                                    else -> entry.type.name
                                }
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = when (entry.type) {
                                    NavigationType.MAIN_SCREEN -> Icons.Outlined.Home
                                    NavigationType.PROJECT_SCREEN -> Icons.Outlined.Folder
                                    NavigationType.GLOBAL_SEARCH -> Icons.Outlined.Search
                                    else -> Icons.Outlined.Info
                                },
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            if (isCurrentEntry) {
                                Icon(
                                    imageVector = Icons.Outlined.RadioButtonChecked,
                                    contentDescription = "Поточна сторінка",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
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