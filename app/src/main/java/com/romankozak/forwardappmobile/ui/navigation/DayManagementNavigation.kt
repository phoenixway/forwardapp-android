package com.romankozak.forwardappmobile.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementScreen

// --- Маршрути для секції управління добою ---

// Головний маршрут для всієї секції (граф навігації)
const val DAY_MANAGEMENT_ROUTE = "day_management_screen"
const val DAY_PLAN_DATE_ARG = "date"

// Внутрішні маршрути для BottomNavigationBar
const val DAY_PLAN_LIST_ROUTE = "day_plan_list"
const val DAY_DASHBOARD_ROUTE = "day_dashboard"
const val DAY_ANALYTICS_ROUTE = "day_analytics"

/**
 * Створює вкладений граф навігації для всієї секції "Управління добою".
 */
fun NavGraphBuilder.dayManagementGraph(navController: NavController) {
    composable(
        route = "$DAY_MANAGEMENT_ROUTE/{$DAY_PLAN_DATE_ARG}",
        arguments = listOf(navArgument(DAY_PLAN_DATE_ARG) { type = NavType.LongType })
    ) {
        DayManagementScreen(mainNavController = navController)
    }
}

/**
 * Допоміжна функція для навігації на секцію управління добою.
 */
fun NavController.navigateToDayManagement(date: Long) {
    this.navigate("$DAY_MANAGEMENT_ROUTE/$date")
}
