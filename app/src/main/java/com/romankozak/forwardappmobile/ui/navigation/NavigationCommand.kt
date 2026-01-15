package com.romankozak.forwardappmobile.ui.navigation

import androidx.navigation.NavOptionsBuilder

sealed interface NavigationCommand {

    // Новий типізований варіант
    data class NavigateTarget(
        val target: NavTarget,
        val builder: (NavOptionsBuilder.() -> Unit)? = null
    ) : NavigationCommand

    // Старий варіант — route як рядок
    data class Navigate(
        val route: String,
        val builder: (NavOptionsBuilder.() -> Unit)? = null
    ) : NavigationCommand

    data class PopBack(
        val key: String? = null,
        val value: Any? = null
    ) : NavigationCommand
}
