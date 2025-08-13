package com.romankozak.forwardappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.goaldetail.GoalDetailScreen
import com.romankozak.forwardappmobile.ui.screens.goaledit.GoalEditScreen
import com.romankozak.forwardappmobile.ui.screens.goallist.GoalListScreen
import com.romankozak.forwardappmobile.ui.screens.goallist.GoalListViewModel
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val syncDataViewModel: SyncDataViewModel by viewModels()
    // Видаляємо db, бо Hilt тепер керує цим
    // private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
            // Hilt сам надасть GoalListViewModel
            val viewModel: GoalListViewModel = hiltViewModel()
            GoalListScreen(
                navController = navController,
                syncDataViewModel = syncDataViewModel,
                viewModel = viewModel
            )
        }

        composable("activity_tracker_screen") {
            ActivityTrackerScreen(navController = navController)
        }

        composable(
            route = "goal_edit_screen/{listId}/{goalId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") { type = NavType.StringType }
            )
        ) {
            // Hilt сам надасть GoalEditViewModel.
            GoalEditScreen(
                navController = navController,
                viewModel = hiltViewModel()
            )
        }

        composable(
            // 1. Змінюємо назву в самому маршруті з {goalListId} на {listId}
            route = "goal_detail_screen/{listId}?goalToHighlight={goalToHighlight}",
            arguments = listOf(
                // 2. Змінюємо назву аргументу з "goalListId" на "listId"
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
            // Hilt сам надасть GlobalSearchViewModel.
            GlobalSearchScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }

        composable("sync_screen") {
            // Цей ViewModel поки що не використовує Hilt, тому залишаємо як є.
            SyncScreen(
                syncDataViewModel = syncDataViewModel,
                onSyncComplete = { navController.popBackStack() }
            )
        }
    }
}