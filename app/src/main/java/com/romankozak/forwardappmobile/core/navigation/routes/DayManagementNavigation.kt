package com.romankozak.forwardappmobile.core.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementScreen

fun NavGraphBuilder.dayManagementGraph(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
) {
    composable(
        route = "$DAY_MANAGEMENT_ROUTE/{$DAY_PLAN_DATE_ARG}?startTab={startTab}",
        arguments =
            listOf(
                navArgument(DAY_PLAN_DATE_ARG) { type = NavType.LongType },
                navArgument("startTab") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val startTab = backStackEntry.arguments?.getString("startTab")
        DayManagementScreen(
            mainNavController = navController,
            navigationManager = navigationManager,
            startTab = startTab,
        )
    }
}

// File: DayManagementNavigation.kt

fun NavController.navigateToDayManagement(
    date: Long,
    startTab: String? = null,
) {
    this.navigate(NavigationRoutes.dayManagement(date = date, startTab = startTab))
}
