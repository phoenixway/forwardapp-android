package com.romankozak.forwardappmobile.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun EnhancedNavigationManager?.navigateOrFallback(
    navController: NavController,
    target: NavTarget,
    recordInHistory: Boolean = false,
    historyTitle: String? = null,
    builder: (NavOptionsBuilder.() -> Unit)? = null,
) {
    if (this != null) {
        navigate(
            target = target,
            builder = builder,
            recordInHistory = recordInHistory,
            historyTitle = historyTitle,
        )
        return
    }

    val route = NavTargetRouter.routeOf(target)
    if (builder != null) {
        navController.navigate(route, builder)
    } else {
        navController.navigate(route)
    }
}
