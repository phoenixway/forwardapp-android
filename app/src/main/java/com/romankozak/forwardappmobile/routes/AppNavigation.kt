// File: AppNavigation.kt - ПОВНА ВИПРАВЛЕНА ВЕРСІЯ

package com.romankozak.forwardappmobile.routes

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.romankozak.forwardappmobile.ui.navigation.AppNavigationViewModel
import com.romankozak.forwardappmobile.ui.navigation.NavigationCommand
import com.romankozak.forwardappmobile.ui.screens.ManageContextsScreen
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.ProjectsScreen
import com.romankozak.forwardappmobile.ui.screens.editlist.EditProjectScreen
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchViewModel
import com.romankozak.forwardappmobile.ui.screens.goaledit.GoalEditScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.noteedit.NoteEditScreen
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.screens.insights.AiInsightsScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import java.net.URLDecoder

// **FIX 1: Створюємо константи для нового графа**
const val MAIN_GRAPH_ROUTE = "main_graph"
const val GOAL_LISTS_ROUTE = "goal_lists_screen"
const val AI_INSIGHTS_ROUTE = "ai_insights_screen"

@Composable
fun AppNavigation(syncDataViewModel: SyncDataViewModel) {
    val navController = rememberNavController()
    val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()

    appNavigationViewModel.initialize()
    val navigationManager = appNavigationViewModel.navigationManager

    LaunchedEffect(navigationManager, navController) {
        navigationManager.navigationCommandFlow.collect { command ->
            when (command) {
                is NavigationCommand.Navigate -> {
                    navController.navigate(command.route, command.builder)
                }
                is NavigationCommand.PopBackStack -> {
                    if (command.key != null && command.value != null) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(command.key, command.value)
                    }
                    navController.popBackStack()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        // **FIX 2: Стартовим маршрутом стає наш новий граф**
        startDestination = MAIN_GRAPH_ROUTE,
    ) {
        // **FIX 3: Огортаємо всі екрани в батьківський граф**
        navigation(
            startDestination = GOAL_LISTS_ROUTE,
            route = MAIN_GRAPH_ROUTE
        ) {
            mainGraph(navController, syncDataViewModel, appNavigationViewModel)
        }
    }
}

// **FIX 4: Виносимо всі екрани в окрему функцію для чистоти**
private fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    syncDataViewModel: SyncDataViewModel,
    appNavigationViewModel: AppNavigationViewModel
) {
    composable(GOAL_LISTS_ROUTE) { backStackEntry ->
        // **FIX 5: Отримуємо ViewModel, прив'язану до БАТЬКІВСЬКОГО ГРАФА**
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MAIN_GRAPH_ROUTE)
        }
        val viewModel: MainScreenViewModel = hiltViewModel(parentEntry)

        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        LaunchedEffect(Unit) {
            if (appNavigationViewModel.navigationManager.getNavigationHistory().isEmpty()) {
                appNavigationViewModel.navigationManager.navigateToMainScreen(isInitial = true)
            }
        }

        MainScreen(
            navController = navController,
            syncDataViewModel = syncDataViewModel,
            viewModel = viewModel,
        )
    }

    composable(
        route = "goal_detail_screen/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}&inboxRecordIdToHighlight={inboxRecordIdToHighlight}",
        arguments = listOf(
            navArgument("listId") { type = NavType.StringType },
            navArgument("goalId") { type = NavType.StringType; nullable = true },
            navArgument("itemIdToHighlight") { type = NavType.StringType; nullable = true },
            navArgument("inboxRecordIdToHighlight") { type = NavType.StringType; nullable = true }
        )
    ) {
        val viewModel: BacklogViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager
        ProjectsScreen(navController = navController, viewModel = viewModel)
    }

    composable(
        "global_search_screen/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
    ) {
        val viewModel: GlobalSearchViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        GlobalSearchScreen(
            viewModel = viewModel,
            navController = navController,
        )
    }

    composable("settings_screen") { backStackEntry ->
        // **FIX 6: Тут також отримуємо ViewModel від батьківського графа**
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MAIN_GRAPH_ROUTE)
        }
        val goalListViewModel: MainScreenViewModel = hiltViewModel(parentEntry)

        val uiState by goalListViewModel.uiState.collectAsStateWithLifecycle()
        val reservedContextCount = uiState.allContexts.count { it.isReserved }

        SettingsScreen(
            planningSettings = uiState.planningSettings,
            initialVaultName = uiState.obsidianVaultName,
            reservedContextCount = reservedContextCount,
            onManageContextsClick = {
                navController.navigate("manage_contexts_screen")
            },
            onBack = { navController.popBackStack() },
            onSave = { showModes, dailyTag, mediumTag, longTag, newVaultName ->
                goalListViewModel.onEvent(
                    MainScreenEvent.SaveSettings(
                        show = showModes,
                        daily = dailyTag,
                        medium = mediumTag,
                        long = longTag,
                        vaultName = newVaultName
                    )
                )
            },
        )
    }

    composable("manage_contexts_screen") { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MAIN_GRAPH_ROUTE)
        }
        val goalListViewModel: MainScreenViewModel = hiltViewModel(parentEntry)

        val uiState by goalListViewModel.uiState.collectAsStateWithLifecycle()

        ManageContextsScreen(
            initialContexts = uiState.allContexts,
            onBack = { navController.popBackStack() },
            onSave = { updatedContexts ->
                goalListViewModel.onEvent(MainScreenEvent.SaveAllContexts(updatedContexts))
                navController.popBackStack()
            },
        )
    }

    composable("activity_tracker_screen") {
        ActivityTrackerScreen(navController = navController)
    }

    composable(
        route = "goal_edit_screen/{listId}?goalId={goalId}",
        arguments =
            listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        GoalEditScreen(
            navController = navController,
            viewModel = hiltViewModel(),
        )
    }

    composable(
        route = "edit_list_screen/{listId}",
        arguments =
            listOf(
                navArgument("listId") { type = NavType.StringType },
            ),
    ) {
        EditProjectScreen(
            navController = navController
        )
    }

    composable("sync_screen") {
        SyncScreen(
            syncDataViewModel = syncDataViewModel,
            onSyncComplete = { navController.popBackStack() },
        )
    }
    composable(
        route = "note_edit_screen/{listId}/{noteId}",
        arguments =
            listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("noteId") { type = NavType.StringType },
            ),
    ) {
        NoteEditScreen(navController = navController)
    }

    composable(
        route = "list_chooser_screen/{title}?currentParentId={currentParentId}&disabledIds={disabledIds}",
        arguments =
            listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("currentParentId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("disabledIds") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry ->
        val viewModel: FilterableListChooserViewModel = hiltViewModel()
        val TAG = "MOVE_DEBUG"

        val title =
            backStackEntry.arguments?.getString("title")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: "Select a list"

        val disabledIds =
            backStackEntry.arguments
                ?.getString("disabledIds")
                ?.split(",")
                ?.toSet() ?: emptySet()
        val currentParentIdArg = backStackEntry.arguments?.getString("currentParentId")
        val currentParentId = if (currentParentIdArg == "root") null else currentParentIdArg

        Log.d(TAG, "[Nav] list_chooser_screen launched.")

        val chooserUiState by viewModel.chooserState.collectAsStateWithLifecycle()
        val filterText by viewModel.filterText.collectAsStateWithLifecycle()
        val expandedIds by viewModel.expandedIds.collectAsStateWithLifecycle()
        val showDescendants by viewModel.showDescendants.collectAsStateWithLifecycle()

        FilterableListChooserScreen(
            title = title,
            filterText = filterText,
            onFilterTextChanged = viewModel::updateFilterText,
            chooserUiState = chooserUiState,
            expandedIds = expandedIds,
            onToggleExpanded = viewModel::toggleExpanded,
            onNavigateBack = { navController.popBackStack() },
            onConfirm = { selectedId ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("list_chooser_result", selectedId ?: "root")

                navController.popBackStack()
            },
            currentParentId = currentParentId,
            disabledIds = disabledIds,
            onAddNewList = viewModel::addNewProject,
            showDescendants = showDescendants,
            onToggleShowDescendants = viewModel::toggleShowDescendants,
        )
    }
    chatScreen(navController)
    dayManagementGraph(navController)
    dayPlanScreen(navController)

    composable(AI_INSIGHTS_ROUTE) {
        AiInsightsScreen(navController = navController)
    }
}