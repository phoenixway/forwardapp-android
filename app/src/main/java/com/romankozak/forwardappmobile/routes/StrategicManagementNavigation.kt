
package com.romankozak.forwardappmobile.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.romankozak.forwardappmobile.ui.screens.strategicmanagement.StrategicManagementScreen

const val STRATEGIC_MANAGEMENT_ROUTE = "strategic_management_screen"

fun NavGraphBuilder.strategicManagementScreen(navController: NavController) {
    composable(STRATEGIC_MANAGEMENT_ROUTE) {
        StrategicManagementScreen(navController = navController)
    }
}

fun NavController.navigateToStrategicManagement() {
    this.navigate(STRATEGIC_MANAGEMENT_ROUTE)
}
