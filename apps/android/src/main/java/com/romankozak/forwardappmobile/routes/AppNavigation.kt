package com.romankozak.forwardappmobile.routes

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.romankozak.forwardappmobile.features.mainscreen.MainScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreen
import com.romankozak.forwardappmobile.routes.PROJECT_ID_ARG
import com.romankozak.forwardappmobile.routes.PROJECT_SCREEN_ROUTE

const val MAIN_SCREEN_ROUTE = "main_screen"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = MAIN_SCREEN_ROUTE,
        ) {
            composable(MAIN_SCREEN_ROUTE) {
                MainScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable(
                route = "$PROJECT_SCREEN_ROUTE/{$PROJECT_ID_ARG}",
                arguments = listOf(navArgument(PROJECT_ID_ARG) { type = NavType.StringType })
            ) { backStackEntry ->
                ProjectScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    projectId = backStackEntry.arguments?.getString(PROJECT_ID_ARG),
                    modifier = TODO(),
                    listState = TODO(),
                )
            }

            composable(
                route = "goal_detail_screen/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}&inboxRecordIdToHighlight={inboxRecordIdToHighlight}&initialViewMode={initialViewMode}",
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
                        navArgument("initialViewMode") {
                            type = NavType.StringType
                            nullable = true
                        },
                    ),
            ) { backStackEntry -> // Add backStackEntry here
                val projectId = backStackEntry.arguments?.getString("listId")

                ProjectScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    projectId = projectId,
                    modifier = TODO(),
                    listState = TODO(),
                )
            }
        }
    }
}
