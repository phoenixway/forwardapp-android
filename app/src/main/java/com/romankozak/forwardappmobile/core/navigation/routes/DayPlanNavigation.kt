package com.romankozak.forwardappmobile.core.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementScreen

const val DAY_PLAN_ROUTE = NavigationRoutes.DAY_PLAN
const val DAY_PLAN_ID_ARG = NavigationRoutes.ARG_DAY_PLAN_ID
const val START_TAB_ARG = NavigationRoutes.ARG_START_TAB

fun NavGraphBuilder.dayManagementScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
) {
    composable(
        route = "$DAY_PLAN_ROUTE/{$DAY_PLAN_ID_ARG}?$START_TAB_ARG={$START_TAB_ARG}",
        arguments =
            listOf(
                navArgument(DAY_PLAN_ID_ARG) { type = NavType.StringType },
                navArgument(START_TAB_ARG) {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry ->
        val startTab = backStackEntry.arguments?.getString(START_TAB_ARG)
        DayManagementScreen(
            mainNavController = navController,
            navigationManager = navigationManager,
            startTab = startTab,
        )
    }
}
