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

        composable(
            "goal_detail_screen/{listId}?goalId={goalId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") { type = NavType.StringType; nullable = true }
            )
        ) {
            // Hilt сам надасть GoalDetailViewModel.
            // SavedStateHandle вже буде включено автоматично.
            GoalDetailScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }

        composable(
            "goal_detail_screen/{goalListId}?goalId={goalToHighlight}", // <-- ВИПРАВЛЕНО
            arguments = listOf(
                navArgument("goalListId") { type = NavType.StringType }, // <-- ВИПРАВЛЕНО
                navArgument("goalToHighlight") { // <-- Також привів до відповідності
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            // Hilt сам надасть GoalDetailViewModel.
            // SavedStateHandle вже буде включено автоматично.
            GoalDetailScreen(
                navController = navController
                // viewModel тут не потрібен, hiltViewModel() зробить все сам
            )
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