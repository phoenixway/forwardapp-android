
package com.romankozak.forwardappmobile.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayPlanScreen


const val DAY_PLAN_ROUTE = "day_plan_screen"
const val DAY_PLAN_ID_ARG = "dayPlanId"

fun NavGraphBuilder.dayPlanScreen(navController: NavController) {
    composable(
        route = "$DAY_PLAN_ROUTE/{$DAY_PLAN_ID_ARG}",
        arguments = listOf(navArgument(DAY_PLAN_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        
        val dayPlanId = backStackEntry.arguments?.getString(DAY_PLAN_ID_ARG) ?: ""

        DayPlanScreen(
            dayPlanId = dayPlanId,
            onNavigateToProject = { projectId ->
                navController.navigate("goal_detail_screen/$projectId")
            },
            onNavigateToBacklog = { task ->
                task.projectId?.let { id ->
                    navController.navigate("goal_detail_screen/$id")
                }
            },
            addTaskTrigger = 0
        )
    }
}


fun NavController.navigateToDayPlan(dayPlanId: String) {
    this.navigate("$DAY_PLAN_ROUTE/$dayPlanId")
}
