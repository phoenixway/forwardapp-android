package com.romankozak.forwardappmobile

import android.util.Log
import androidx.compose.runtime.Composable
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
import com.romankozak.forwardappmobile.ui.navigation.chatScreen
import com.romankozak.forwardappmobile.ui.screens.ManageContextsScreen
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalDetailScreen
import com.romankozak.forwardappmobile.ui.screens.backlogs.GoalListScreen
import com.romankozak.forwardappmobile.ui.screens.backlogs.GoalListViewModel
import com.romankozak.forwardappmobile.ui.screens.editlist.EditListScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.goaledit.GoalEditScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.ui.screens.noteedit.NoteEditScreen
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(
    syncDataViewModel: SyncDataViewModel,
) {
    val navController = rememberNavController()
    // val navGraphRoute = "app_graph" // Більше не потрібен для цього механізму


    NavHost(
        navController = navController,
        startDestination = "goal_lists_screen",
        // route = navGraphRoute // Більше не потрібен для цього механізму

    ) {
        composable("goal_lists_screen") {
            val viewModel: GoalListViewModel = hiltViewModel()
            GoalListScreen(
                navController = navController,
                syncDataViewModel = syncDataViewModel,
                viewModel = viewModel
            )
        }

        composable("settings_screen") { backStackEntry ->
            val goalListViewModel: GoalListViewModel = hiltViewModel(
                remember(backStackEntry) {
                    navController.getBackStackEntry("goal_lists_screen")
                }
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
                        newVaultName
                    )
                }
            )
        }

        composable("manage_contexts_screen") { backStackEntry ->
            val goalListViewModel: GoalListViewModel = hiltViewModel(
                remember(backStackEntry) {
                    navController.getBackStackEntry("goal_lists_screen")
                }
            )
            val allContexts by goalListViewModel.allContextsForDialog.collectAsState()

            ManageContextsScreen(
                initialContexts = allContexts,
                onBack = { navController.popBackStack() },
                onSave = { updatedContexts ->
                    goalListViewModel.saveAllContexts(updatedContexts)
                    navController.popBackStack()
                }
            )
        }

        composable("activity_tracker_screen") {
            ActivityTrackerScreen(navController = navController)
        }

        composable(
            route = "goal_edit_screen/{listId}?goalId={goalId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            GoalEditScreen(
                navController = navController,
                viewModel = hiltViewModel()
            )
        }

        composable(
            route = "goal_detail_screen/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}",
            arguments = listOf(
                navArgument("listId") {
                    type = NavType.StringType
                },
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("itemIdToHighlight") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            GoalDetailScreen(navController = navController)
        }

        composable(
            route = "edit_list_screen/{listId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditListScreen(
                navController = navController,
                listId = backStackEntry.arguments?.getString("listId") ?: ""
            )
        }

        composable(
            "global_search_screen/{query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) {
            GlobalSearchScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }

        composable("sync_screen") {
            SyncScreen(
                syncDataViewModel = syncDataViewModel,
                onSyncComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = "note_edit_screen/{listId}/{noteId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("noteId") { type = NavType.StringType }
            )
        ) {
            NoteEditScreen(navController = navController)
        }

        composable(
            route = "list_chooser_screen/{title}?disabledIds={disabledIds}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("disabledIds") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val viewModel: FilterableListChooserViewModel = hiltViewModel()
            val TAG = "AddSublistDebug"

            val title = backStackEntry.arguments?.getString("title")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: "Виберіть список"
            val disabledIds = backStackEntry.arguments?.getString("disabledIds")?.split(",")?.toSet() ?: emptySet()

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
                    // --- ПОЧАТОК КЛЮЧОВИХ ЗМІН ---
                    Log.d(TAG, "[ChooserScreen] onConfirm: User selected ID: '$selectedId'")
                    // 1. Встановлюємо результат для попереднього екрана через SavedStateHandle
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("list_chooser_result", selectedId ?: "") // Передаємо ID або "" для root

                    // 2. Повертаємось на попередній екран
                    navController.popBackStack()
                    Log.d(TAG, "[ChooserScreen] Result set and popped backstack.")
                    // --- КІНЕЦЬ КЛЮЧОВИХ ЗМІН ---
                },
                currentParentId = null,
                disabledIds = disabledIds,
                onAddNewList = viewModel::addNewList,
                showDescendants = showDescendants,
                onToggleShowDescendants = viewModel::toggleShowDescendants
            )
        }
        chatScreen(navController)
    }
}