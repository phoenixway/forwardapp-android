package com.romankozak.forwardappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.screens.ManageContextsScreen
import com.romankozak.forwardappmobile.ui.screens.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.goaldetail.GoalDetailScreen
import com.romankozak.forwardappmobile.ui.screens.goaledit.GoalEditScreen
import com.romankozak.forwardappmobile.ui.screens.goallist.GoalListScreen
import com.romankozak.forwardappmobile.ui.screens.goallist.GoalListViewModel
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.ui.screens.noteedit.NoteEditScreen
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val syncDataViewModel: SyncDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ForwardAppMobileTheme {
                AppNavigation(syncDataViewModel = syncDataViewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(
    syncDataViewModel: SyncDataViewModel,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "goal_lists_screen"
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
            route = "goal_detail_screen/{listId}?goalToHighlight={goalToHighlight}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalToHighlight") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            GoalDetailScreen(navController = navController)
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
        // File: MainActivity.kt

// Файл: MainActivity.kt

        composable("list_chooser_screen/{currentParentId}?disabledIds={disabledIds}") { backStackEntry ->
            val viewModel: FilterableListChooserViewModel = hiltViewModel()
            val currentParentId = backStackEntry.arguments?.getString("currentParentId")
            val disabledIds = backStackEntry.arguments?.getString("disabledIds")?.split(",")?.toSet() ?: emptySet()

            val chooserUiState by viewModel.chooserState.collectAsState()
            val expandedIds by viewModel.expandedIds.collectAsState()
            // --- ПОЧАТОК ЗМІН: Отримуємо новий стан з ViewModel ---
            val showDescendants by viewModel.showDescendants.collectAsState()
            // --- КІНЕЦЬ ЗМІН ---

            FilterableListChooserScreen(
                title = "Виберіть список",
                filterText = viewModel.filterText.collectAsState().value,
                onFilterTextChanged = viewModel::updateFilterText,
                chooserUiState = chooserUiState,
                expandedIds = expandedIds,
                onToggleExpanded = viewModel::toggleExpanded,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onConfirm = { selectedId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedListId", selectedId)
                    navController.popBackStack()
                },
                currentParentId = currentParentId,
                disabledIds = disabledIds,
                onAddNewList = viewModel::addNewList,
                // --- ПОЧАТОК ЗМІН: Передаємо нові параметри в UI ---
                showDescendants = showDescendants,
                onToggleShowDescendants = viewModel::toggleShowDescendants
                // --- КІНЕЦЬ ЗМІН ---
            )
        }
    }}

