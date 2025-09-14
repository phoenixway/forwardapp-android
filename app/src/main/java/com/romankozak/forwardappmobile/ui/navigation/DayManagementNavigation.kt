package com.romankozak.forwardappmobile.ui.navigation // Приклад шляху

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementScreen
import com.romankozak.forwardappmobile.ui.navigation.DAY_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.ui.navigation.DAY_PLAN_DATE_ARG

fun NavGraphBuilder.dayManagementGraph(navController: NavController) {
    composable(
        // Маршрут очікує дату у форматі Long (milliseconds)
        route = "$DAY_MANAGEMENT_ROUTE/{$DAY_PLAN_DATE_ARG}",
        arguments = listOf(navArgument(DAY_PLAN_DATE_ARG) { type = NavType.LongType })
    ) {
        // Тепер ця функція існує, і помилки не буде
        DayManagementScreen(mainNavController = navController)
    }
}

// Функція для зручної навігації на цей екран
fun NavController.navigateToDayManagement(date: Long) {
    this.navigate("$DAY_MANAGEMENT_ROUTE/$date")
}