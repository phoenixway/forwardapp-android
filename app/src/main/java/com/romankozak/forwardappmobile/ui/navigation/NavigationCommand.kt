package com.romankozak.forwardappmobile.ui.navigation

import androidx.navigation.NavOptionsBuilder

sealed class NavigationCommand {

    data class Navigate(
        val route: String,
        val builder: (NavOptionsBuilder.() -> Unit)? = null
    ) : NavigationCommand()

    data class PopBackStack(
        val key: String? = null,
        val value: String? = null
    ) : NavigationCommand()
}
