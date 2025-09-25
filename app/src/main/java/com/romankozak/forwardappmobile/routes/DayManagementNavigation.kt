package com.romankozak.forwardappmobile.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementScreen

fun NavGraphBuilder.dayManagementGraph(navController: NavController) {
    composable(
        
        route = "$DAY_MANAGEMENT_ROUTE/{$DAY_PLAN_DATE_ARG}",
        arguments = listOf(navArgument(DAY_PLAN_DATE_ARG) { type = NavType.LongType }),
    ) {
        
        DayManagementScreen(mainNavController = navController)
    }
}


// File: DayManagementNavigation.kt

fun NavController.navigateToDayManagement(date: Long, startTab: String? = null) {
    // Формуємо маршрут, додаючи startTab як query-параметр, якщо він існує
    val route = if (startTab != null) {
        "$DAY_MANAGEMENT_ROUTE/$date?startTab=$startTab"
    } else {
        "$DAY_MANAGEMENT_ROUTE/$date"
    }
    this.navigate(route)
}
