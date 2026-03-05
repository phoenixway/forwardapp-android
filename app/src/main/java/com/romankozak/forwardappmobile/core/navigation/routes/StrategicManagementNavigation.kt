package com.romankozak.forwardappmobile.core.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.strategicmanagement.StrategicManagementScreen

const val STRATEGIC_MANAGEMENT_ROUTE = NavigationRoutes.STRATEGIC_MANAGEMENT

fun NavGraphBuilder.strategicManagementScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
) {
    composable(STRATEGIC_MANAGEMENT_ROUTE) {
        StrategicManagementScreen(
            navController = navController,
            navigationManager = navigationManager,
        )
    }
}

fun NavController.navigateToStrategicManagement() {
    this.navigate(STRATEGIC_MANAGEMENT_ROUTE)
}
