package com.romankozak.forwardappmobile.features.navigation

import androidx.navigation.NavHostController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNavigationDispatcher @Inject constructor() : NavigationDispatcher {

    private var navController: NavHostController? = null

    fun attach(navController: NavHostController) {
        this.navController = navController
    }

    override fun navigate(route: String) {
        navController?.navigate(route)
    }

    fun navigate(target: NavTarget) {
        val route = NavTargetRouter.routeOf(target)
        navController?.navigate(route)
    }

    override fun popBackStack(key: String?, value: String?) {
        navController?.popBackStack()
    }
}
