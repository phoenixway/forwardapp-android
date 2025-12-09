package com.romankozak.forwardappmobile.ui.navigation

interface NavigationDispatcher {
    fun navigate(route: String)
    fun popBackStack(key: String? = null, value: String? = null)
}
