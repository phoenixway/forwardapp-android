package com.romankozak.forwardappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi // <-- ДОДАЙТЕ ЦЕЙ ІМПОРТ
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme

class MainActivity : ComponentActivity() {

    private val syncDataViewModel: SyncDataViewModel by viewModels()
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForwardAppMobileTheme {
                AppNavigation(
                    syncDataViewModel = syncDataViewModel,
                    db = db
                )
            }
        }
    }
}

// ДОДАЙТЕ АНОТАЦІЮ ТУТ
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigation(
    syncDataViewModel: SyncDataViewModel,
    db: AppDatabase
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "goal_lists_screen"
    ) {
        composable("goal_lists_screen") {
            GoalListScreen(
                navController = navController,
                syncDataViewModel = syncDataViewModel
            )
        }

        composable(
            "goal_detail_screen/{listId}?goalId={goalId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId")
            if (listId != null) {
                val goalIdToHighlight = backStackEntry.arguments?.getString("goalId")
                val detailViewModelFactory = GoalDetailViewModelFactory(db.goalDao(), listId, goalIdToHighlight)
                val detailViewModel: GoalDetailViewModel = viewModel(factory = detailViewModelFactory)

                GoalDetailScreen(
                    viewModel = detailViewModel,
                    navController = navController
                )
            }
        }

        composable(
            "global_search_screen/{query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query")
            if (query != null) {
                val searchViewModelFactory = GlobalSearchViewModelFactory(db.goalDao(), query)
                val searchViewModel: GlobalSearchViewModel = viewModel(factory = searchViewModelFactory)

                GlobalSearchScreen(viewModel = searchViewModel, navController = navController)
            }
        }

        composable("sync_screen") {
            SyncScreen(
                syncDataViewModel = syncDataViewModel,
                onSyncComplete = { navController.popBackStack() }
            )
        }
    }
}