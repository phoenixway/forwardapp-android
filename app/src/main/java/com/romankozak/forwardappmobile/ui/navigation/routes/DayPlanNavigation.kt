// file: ui/navigation/DayPlanNavigation.kt
package com.romankozak.forwardappmobile.ui.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayPlanScreen

// Константи для нового маршруту
const val DAY_PLAN_ROUTE = "day_plan_screen"
const val DAY_PLAN_ID_ARG = "dayPlanId"

fun NavGraphBuilder.dayPlanScreen(navController: NavController) {
    composable(
        route = "$DAY_PLAN_ROUTE/{$DAY_PLAN_ID_ARG}",
        arguments = listOf(navArgument(DAY_PLAN_ID_ARG) { type = NavType.StringType })
    ) { backStackEntry ->
        // Отримуємо ID плану з аргументів маршруту
        val dayPlanId = backStackEntry.arguments?.getString(DAY_PLAN_ID_ARG) ?: ""

        DayPlanScreen(
            dayPlanId = dayPlanId,
            onNavigateBack = { navController.popBackStack() },
            // Маршрут "goal_detail_screen/{listId}" взято з вашого AppNavigation.kt
            onNavigateToProject = { projectId ->
                navController.navigate("goal_detail_screen/$projectId")
            },
            // --- ВИРІШЕННЯ ПОМИЛКИ: Додано реалізацію onNavigateToBacklog ---
            onNavigateToBacklog = { task ->
                // Завдання може бути пов'язане з проєктом (підсписком).
                // Якщо так, ми використовуємо його ID для навігації на екран беклогу.
                // Маршрут "goal_detail_screen/" - це екран беклогу для конкретного проєкту.
                task.projectId?.let { id ->
                    navController.navigate("goal_detail_screen/$id")
                }
                // Якщо у завдання є тільки goalId, але немає projectId, для навігації
                // може знадобитись додатковий запит до БД для пошуку батьківського проєкту.
                // Поточна реалізація обробляє прямий зв'язок "завдання-проєкт".
            }
        )
    }
}

// Допоміжна функція для зручного переходу на цей екран
fun NavController.navigateToDayPlan(dayPlanId: String) {
    this.navigate("$DAY_PLAN_ROUTE/$dayPlanId")
}