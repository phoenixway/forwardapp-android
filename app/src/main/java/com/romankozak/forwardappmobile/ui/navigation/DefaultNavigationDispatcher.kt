package com.romankozak.forwardappmobile.ui.navigation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Глобальний диспетчер навігації.
 * ViewModel-и інʼєктують NavigationDispatcher і викликають navigate / popBackStack.
 * AppNavigationViewModel реєструється як handler і обробляє ці виклики через EnhancedNavigationManager.
 */
interface NavigationHandler {
    fun handleNavigate(route: String)
    fun handlePopBackStack(key: String? = null, value: String? = null)
}

@Singleton
class DefaultNavigationDispatcher @Inject constructor() : NavigationDispatcher {

    @Volatile
    private var handler: NavigationHandler? = null

    fun setHandler(handler: NavigationHandler) {
        this.handler = handler
    }

    override fun navigate(route: String) {
        handler?.handleNavigate(route)
            ?: error("NavigationHandler is not set. Did you call AppNavigationViewModel.initialize()?")
    }

    override fun popBackStack(key: String?, value: String?) {
        handler?.handlePopBackStack(key, value)
            ?: error("NavigationHandler is not set. Did you call AppNavigationViewModel.initialize()?")
    }
}