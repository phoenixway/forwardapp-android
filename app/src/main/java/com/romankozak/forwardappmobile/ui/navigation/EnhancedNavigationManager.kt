package com.romankozak.forwardappmobile.ui.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavOptionsBuilder
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.NavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NavigationResult(val key: String, val value: String)

class EnhancedNavigationManager(
    savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope,
) {

    companion object {
        private const val TAG = "Nav_DEBUG"
    }

    private val historyManager = NavigationHistoryManager(savedStateHandle, scope)

    private val _navigationChannel = Channel<NavigationCommand>()
    val navigationCommandFlow = _navigationChannel.receiveAsFlow()

    private val _navigationResults = MutableSharedFlow<NavigationResult>()
    val navigationResults: SharedFlow<NavigationResult> = _navigationResults.asSharedFlow()

    val canGoBack: StateFlow<Boolean> = historyManager.canGoBack
    val canGoForward: StateFlow<Boolean> = historyManager.canGoForward
    val currentEntry: StateFlow<NavigationEntry?> = historyManager.currentEntry

    private val _showNavigationMenu = MutableStateFlow(false)
    val showNavigationMenu: StateFlow<Boolean> = _showNavigationMenu.asStateFlow()

    fun navigate(route: String, builder: (NavOptionsBuilder.() -> Unit)? = null) {
        sendNavigationCommand(NavigationCommand.Navigate(route, builder))
    }

    fun navigateToProjectHierarchyScreen(isInitial: Boolean = false) {
        val entry = NavigationEntry.createProjectHierarchyScreen()
        historyManager.addEntry(entry)

        if (!isInitial) {
            sendNavigationCommand(NavigationCommand.Navigate("goal_lists_screen"))
        }
    }

    fun navigateToProject(
        projectId: String,
        projectName: String,
    ) {
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
        if (canGoBack.value) {
            historyManager.goBack()
            sendNavigationCommand(NavigationCommand.PopBackStack())
        }
    }

    fun goBackWithResult(key: String, value: String) {
        scope.launch {
            _navigationResults.emit(NavigationResult(key, value))
        }
        sendNavigationCommand(NavigationCommand.PopBackStack(key, value))
        historyManager.goBack()
    }

    fun goForward() {
        val entry = historyManager.goForward()
        if (entry != null) navigateToEntry(entry)
    }

    fun showNavigationMenu() {
        _showNavigationMenu.value = true
    }

    fun hideNavigationMenu() {
        _showNavigationMenu.value = false
    }

    fun navigateToHistoryEntry(index: Int) {
        val entry = historyManager.goToEntry(index)
        if (entry != null) navigateToEntry(entry)
        hideNavigationMenu()
    }

    fun getNavigationHistory(): List<NavigationEntry> = historyManager.getFullHistory()

    fun updateCurrentEntry(updatedTitle: String) {
        val current = currentEntry.value ?: return
        historyManager.updateCurrentEntry(current.copy(title = updatedTitle))
    }

    fun sendResult(key: String, value: String) {
        scope.launch { _navigationResults.emit(NavigationResult(key, value)) }
    }

    fun clearHistory() {
        historyManager.clearHistory()
    }

    fun navigateHome() {
        historyManager.clearHistory()

        sendNavigationCommand(
            NavigationCommand.Navigate("goal_lists_screen") {
                popUpTo("goal_lists_screen") { inclusive = true }
                launchSingleTop = true
            }
        )
    }

    private fun navigateToEntry(entry: NavigationEntry) {
        Log.d(TAG, "Navigating to history entry: ${entry.type} - ${entry.title}")

        val command = when (entry.type) {
            NavigationType.PROJECT_HIERARCHY_SCREEN ->
                NavigationCommand.Navigate("goal_lists_screen") {
                    popUpTo("goal_lists_screen") { inclusive = false }
                }

            NavigationType.PROJECT_SCREEN ->
                NavigationCommand.Navigate("goal_detail_screen/${entry.id}")

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
        scope.launch { _navigationResults.emit(NavigationResult(key, value)) }

        historyManager.clearHistory()

        val entry = NavigationEntry.createProjectHierarchyScreen()
        historyManager.addEntry(entry)

        sendNavigationCommand(
            NavigationCommand.Navigate("goal_lists_screen") {
                popUpTo("goal_lists_screen") { inclusive = true }
                launchSingleTop = true
            }
        )
    }
}
