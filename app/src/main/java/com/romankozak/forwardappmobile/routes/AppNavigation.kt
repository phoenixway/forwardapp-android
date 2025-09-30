package com.romankozak.forwardappmobile.routes

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import androidx.navigation.navigation
import com.romankozak.forwardappmobile.ui.navigation.AppNavigationViewModel
import com.romankozak.forwardappmobile.ui.navigation.NavigationCommand
import com.romankozak.forwardappmobile.ui.screens.ManageContextsScreen
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.screens.customlist.UnifiedCustomListScreen
import com.romankozak.forwardappmobile.ui.screens.editlist.EditProjectScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchViewModel
import com.romankozak.forwardappmobile.ui.screens.goaledit.GoalEditScreen
import com.romankozak.forwardappmobile.ui.screens.insights.AiInsightsScreen
import com.romankozak.forwardappmobile.ui.screens.inbox.InboxEditorScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreen
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.EditTaskScreen
import com.romankozak.forwardappmobile.ui.screens.noteedit.NoteEditScreen
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.ProjectsScreen
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import java.net.URLDecoder


const val MAIN_GRAPH_ROUTE = "main_graph"
const val GOAL_LISTS_ROUTE = "goal_lists_screen"
const val AI_INSIGHTS_ROUTE = "ai_insights_screen"

@OptIn(ExperimentalSharedTransitionApi::class)
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

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = MAIN_GRAPH_ROUTE,
        ) {
            // Єдиний граф верхнього рівня, як у вашій структурі
            navigation(
                startDestination = GOAL_LISTS_ROUTE,
                route = MAIN_GRAPH_ROUTE,
            ) {
                mainGraph(
                    navController,
                    syncDataViewModel,
                    appNavigationViewModel,
                    this@SharedTransitionLayout
                )
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
private fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    syncDataViewModel: SyncDataViewModel,
    appNavigationViewModel: AppNavigationViewModel,
    sharedTransitionScope: SharedTransitionScope,
) {
    composable(GOAL_LISTS_ROUTE) { backStackEntry ->

        val parentEntry =
            remember(backStackEntry) {
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
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this
        )
    }

    composable(
        route = "goal_detail_screen/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}&inboxRecordIdToHighlight={inboxRecordIdToHighlight}",
        arguments =
            listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("itemIdToHighlight") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("inboxRecordIdToHighlight") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry -> // Add backStackEntry here
        val viewModel: BacklogViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        // FIX: Extract the 'listId' argument from the route and assign it to a variable.
        val projectId = backStackEntry.arguments?.getString("listId")

        ProjectsScreen(
            navController = navController,
            viewModel = viewModel,
            projectId = projectId, // Now 'projectId' is a resolved reference.
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this
        )
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

        val parentEntry =
            remember(backStackEntry) {
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
                        vaultName = newVaultName,
                    ),
                )
            },
        )
    }

    composable("manage_contexts_screen") { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
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
            navController = navController,
        )
    }

    composable("sync_screen") {
        SyncScreen(
            syncDataViewModel = syncDataViewModel,
            onSyncComplete = { navController.popBackStack() },
        )
    }

    composable(
        route = "note_edit_screen?projectId={projectId}&noteId={noteId}",
        arguments =
            listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        NoteEditScreen(navController = navController)
    }

    // Об'єднаний екран для перегляду/редагування існуючого списку
    composable(
        route = "custom_list_screen/{listId}",
        arguments = listOf(
            navArgument("listId") { type = NavType.StringType }
        ),
    ) { backStackEntry ->
        val listId = backStackEntry.arguments?.getString("listId")

        UnifiedCustomListScreen(
            navController = navController,
        )
    }

    // Об'єднаний екран для створення нового списку
    composable(
        route = "custom_list_create_screen/{projectId}",
        arguments = listOf(
            navArgument("projectId") { type = NavType.StringType }
        ),
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId")

        UnifiedCustomListScreen(
            navController = navController,
        )
    }

    // Залишаємо старий роут для зворотної сумісності, але переспрямовуємо на новий
    composable(
        route = "custom_list_edit_screen?projectId={projectId}&listId={listId}",
        arguments =
            listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("listId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId")
        val listId = backStackEntry.arguments?.getString("listId")

        UnifiedCustomListScreen(
            navController = navController,
        )
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
    dayManagementScreen(navController)
    strategicManagementScreen(navController)

    composable(
        route = "attachments_screen/{listId}",
        arguments = listOf(navArgument("listId") { type = NavType.StringType })
    ) { backStackEntry ->
        val viewModel: BacklogViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        LaunchedEffect(Unit) {
            backStackEntry.savedStateHandle.getStateFlow<String?>("list_chooser_result", null)
                .collect { result ->
                    if (result != null) {
                        viewModel.onListChooserResult(result)
                        backStackEntry.savedStateHandle.remove<String>("list_chooser_result")
                    }
                }
        }

        com.romankozak.forwardappmobile.ui.screens.projectscreen.ProjectAttachmentsScreen(
            navController = navController,
            projectId = backStackEntry.arguments?.getString("listId")
        )
    }

    composable(AI_INSIGHTS_ROUTE) {
        AiInsightsScreen(navController = navController)
    }

    composable("reminders_screen") {
        com.romankozak.forwardappmobile.ui.screens.reminders.RemindersScreen(navController = navController)
    }

    composable(
        route = "edit_task_screen/{taskId}",
        arguments = listOf(navArgument("taskId") { type = NavType.StringType })
    ) {
        EditTaskScreen(onNavigateUp = { navController.navigateUp() })
    }

    composable(
        route = "inbox_editor_screen/{inboxId}",
        arguments = listOf(navArgument("inboxId") { type = NavType.StringType })
    ) {
        InboxEditorScreen(navController = navController)
    }
}