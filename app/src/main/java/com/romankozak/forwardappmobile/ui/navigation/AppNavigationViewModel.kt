package com.romankozak.forwardappmobile.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationDispatcher: DefaultNavigationDispatcher
) : ViewModel(), NavigationHandler {

    val navigationManager = EnhancedNavigationManager(savedStateHandle, viewModelScope)

    /**
     * Викликається з AppNavigation() перед побудовою NavHost.
     * Тут ми реєструємо себе як handler глобального NavigationDispatcher.
     */
    fun initialize() {
        navigationDispatcher.setHandler(this)
    }

    override fun handleNavigate(route: String) {
        navigationManager.navigate(route)
    }

    override fun handlePopBackStack(key: String?, value: String?) {
        if (key != null && value != null) {
            navigationManager.goBackWithResult(key, value)
        } else {
            navigationManager.goBack()
        }
    }
}