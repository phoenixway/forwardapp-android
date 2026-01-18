package com.romankozak.forwardappmobile.core.navigation

interface NavigationDispatcher {
    fun navigate(route: String)
    fun popBackStack(key: String? = null, value: String? = null)
}
