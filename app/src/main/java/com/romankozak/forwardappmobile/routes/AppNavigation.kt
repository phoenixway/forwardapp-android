package com.romankozak.forwardappmobile.routes

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.navigation.AppNavigationViewModel
import com.romankozak.forwardappmobile.ui.navigation.NavigationCommand // **NEW: Імпортуємо навігаційні команди**
import com.romankozak.forwardappmobile.ui.screens.ManageContextsScreen
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.screens.backlog.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.backlog.ProjectsScreen
import com.romankozak.forwardappmobile.ui.screens.editlist.EditProjectScreen
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchViewModel
import com.romankozak.forwardappmobile.ui.screens.goaledit.GoalEditScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.noteedit.NoteEditScreen
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(syncDataViewModel: SyncDataViewModel) {
    val navController = rememberNavController()
    // 1. Отримуємо AppNavigationViewModel, який живе разом з NavHost
    val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()

    // 2. Ініціалізуємо менеджер (тепер без прямої залежності від NavController)
    appNavigationViewModel.initialize()
    val navigationManager = appNavigationViewModel.navigationManager

    // --- NEW: Центральний обробник навігаційних команд ---
    // Цей ефект слухає потік команд від менеджера і виконує реальну навігацію.
    // Це єдине місце, де викликаються методи navController для навігації.
    LaunchedEffect(navigationManager, navController) {
        navigationManager.navigationCommandFlow.collect { command ->
            when (command) {
                is NavigationCommand.Navigate -> {
                    navController.navigate(command.route, command.builder)
                }
                is NavigationCommand.PopBackStack -> {
                    // Якщо потрібно передати результат на попередній екран
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
        startDestination = "goal_lists_screen",
    ) {
        composable("goal_lists_screen") {
            val viewModel: MainScreenViewModel = hiltViewModel()

            // 3. Передаємо єдиний екземпляр менеджера у ViewModel екрану
            viewModel.enhancedNavigationManager = navigationManager

            // Додаємо головний екран в історію при першому запуску (логіка залишається)
            LaunchedEffect(Unit) {
                if (navigationManager.getNavigationHistory().isEmpty()) {
                    navigationManager.navigateToMainScreen(isInitial = true)
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

            // 4. Передаємо той самий екземпляр менеджера у BacklogViewModel
            viewModel.enhancedNavigationManager = navigationManager

            ProjectsScreen(navController = navController, viewModel = viewModel)
        }

        composable(
            "global_search_screen/{query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType }),
        ) {
            val viewModel: GlobalSearchViewModel = hiltViewModel()
            // 5. Передаємо менеджер у ViewModel глобального пошуку
            viewModel.enhancedNavigationManager = navigationManager

            GlobalSearchScreen(
                viewModel = viewModel,
                navController = navController,
            )
        }

        // --- Решта екранів залишаються здебільшого без змін ---

        composable("settings_screen") { backStackEntry ->
            val goalListViewModel: MainScreenViewModel =
                hiltViewModel(
                    remember(backStackEntry) {
                        navController.getBackStackEntry("goal_lists_screen")
                    },
                )

            val planningSettings by goalListViewModel.planningSettingsState.collectAsState()
            val vaultName by goalListViewModel.obsidianVaultName.collectAsState()
            val allContexts by goalListViewModel.allContextsForDialog.collectAsState()
            val reservedContextCount = allContexts.count { it.isReserved }

            SettingsScreen(
                planningSettings = planningSettings,
                initialVaultName = vaultName,
                reservedContextCount = reservedContextCount,
                onManageContextsClick = {
                    navController.navigate("manage_contexts_screen")
                },
                onBack = { navController.popBackStack() },
                onSave = { showModes, dailyTag, mediumTag, longTag, newVaultName ->
                    goalListViewModel.saveSettings(
                        showModes,
                        dailyTag,
                        mediumTag,
                        longTag,
                        newVaultName,
                    )
                },
            )
        }

        composable("manage_contexts_screen") { backStackEntry ->
            val goalListViewModel: MainScreenViewModel =
                hiltViewModel(
                    remember(backStackEntry) {
                        navController.getBackStackEntry("goal_lists_screen")
                    },
                )
            val allContexts by goalListViewModel.allContextsForDialog.collectAsState()

            ManageContextsScreen(
                initialContexts = allContexts,
                onBack = { navController.popBackStack() },
                onSave = { updatedContexts ->
                    goalListViewModel.saveAllContexts(updatedContexts)
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
    }
}