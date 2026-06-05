package com.romankozak.forwardappmobile.core.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavOptionsBuilder
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.NavigationType
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    val history: StateFlow<List<NavigationEntry>> = historyManager.history

    private val _showNavigationMenu = MutableStateFlow(false)
    val showNavigationMenu: StateFlow<Boolean> = _showNavigationMenu.asStateFlow()

    fun navigate(
        route: String,
        builder: (NavOptionsBuilder.() -> Unit)? = null,
        historyEntry: NavigationEntry? = null,
    ) {
        historyEntry?.let(historyManager::addEntry)
        sendNavigationCommand(NavigationCommand.Navigate(route, builder))
    }

    fun navigate(
        target: NavTarget,
        builder: (NavOptionsBuilder.() -> Unit)? = null,
        recordInHistory: Boolean = false,
        historyTitle: String? = null,
    ) {
        if (recordInHistory) {
            historyEntryForTarget(target, historyTitle)?.let(historyManager::addEntry)
        }
        sendNavigationCommand(NavigationCommand.NavigateTarget(target, builder))
    }

    fun navigateToProjectHierarchyScreen(isInitial: Boolean = false) {
        if (!isInitial) {
            navigate(
                target = NavTarget.ContextHierarchy(),
                recordInHistory = true,
            )
        }
    }

    fun navigateToProject(
        projectId: String,
        projectName: String,
    ) {
        navigate(
            target = NavTarget.ContextDetail(contextId = projectId),
            recordInHistory = true,
            historyTitle = projectName,
        )
    }

    fun navigateToGlobalSearch(query: String) {
        navigate(
            target = NavTarget.GlobalSearch(query),
            recordInHistory = true,
        )
    }

    fun goBack() {
        if (canGoBack.value) {
            historyManager.goBack()
            sendNavigationCommand(NavigationCommand.PopBack())
        }
    }

    fun goBackWithResult(
        key: String,
        value: String,
    ) {
        scope.launch { _navigationResults.emit(NavigationResult(key, value)) }
        sendNavigationCommand(NavigationCommand.PopBack(key, value))
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

    fun sendResult(
        key: String,
        value: String,
    ) {
        scope.launch { _navigationResults.emit(NavigationResult(key, value)) }
    }

    fun clearHistory() {
        historyManager.clearHistory()
    }

    fun navigateHome() {
        historyManager.clearHistory()

        navigate(
            route = "goal_lists_screen",
            builder = {
                popUpTo("goal_lists_screen") { inclusive = true }
                launchSingleTop = true
            },
        )
    }

    private fun navigateToEntry(entry: NavigationEntry) {
        Log.d(TAG, "Navigating to history entry: ${entry.type} - ${entry.title}")

        val command =
            when (entry.type) {
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
        scope.launch { _navigationChannel.send(command) }
    }

    fun navigateHomeWithResult(
        key: String,
        value: String,
    ) {
        scope.launch { _navigationResults.emit(NavigationResult(key, value)) }

        historyManager.clearHistory()

        val entry = NavigationEntry.createProjectHierarchyScreen()
        historyManager.addEntry(entry)

        navigate(
            route = "goal_lists_screen",
            builder = {
                popUpTo("goal_lists_screen") { inclusive = true }
                launchSingleTop = true
            },
        )
    }

    private fun historyEntryForTarget(
        target: NavTarget,
        titleOverride: String?,
    ): NavigationEntry? {
        val route = NavTargetRouter.routeOf(target)
        return when (target) {
            is NavTarget.ContextHierarchy ->
                NavigationEntry(
                    type = NavigationType.PROJECT_HIERARCHY_SCREEN,
                    id = "main",
                    title = titleOverride ?: "Orientations",
                    route = NavigationRoutes.GOAL_LISTS,
                )

            is NavTarget.ContextDetail ->
                NavigationEntry(
                    type = NavigationType.PROJECT_SCREEN,
                    id = target.contextId,
                    title = titleOverride ?: "Context",
                    route = route,
                )

            is NavTarget.GlobalSearch ->
                NavigationEntry(
                    type = NavigationType.GLOBAL_SEARCH,
                    id = "search_${target.query}",
                    title = titleOverride ?: "Search: ${target.query}",
                    route = route,
                )

            else -> null
        }
    }
}
