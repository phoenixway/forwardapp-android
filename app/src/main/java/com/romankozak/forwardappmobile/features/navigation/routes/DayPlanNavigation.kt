package com.romankozak.forwardappmobile.features.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.features.daymanagement.presentation.DayManagementScreen

const val DAY_PLAN_ROUTE = "day_plan_screen"
const val DAY_PLAN_ID_ARG = "dayPlanId"
const val START_TAB_ARG = "startTab"

fun NavGraphBuilder.dayManagementScreen(navController: NavController) {
    composable(
        route = "$DAY_PLAN_ROUTE/{$DAY_PLAN_ID_ARG}?$START_TAB_ARG={$START_TAB_ARG}",
        arguments = listOf(
            navArgument(DAY_PLAN_ID_ARG) { type = NavType.StringType },
            navArgument(START_TAB_ARG) {
                type = NavType.StringType
                nullable = true
            }
        ),
    ) { backStackEntry ->
        val startTab = backStackEntry.arguments?.getString(START_TAB_ARG)
        DayManagementScreen(
            mainNavController = navController,
            startTab = startTab
        )
    }
}

