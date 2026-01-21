package com.romankozak.forwardappmobile.core.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationDispatcher: DefaultNavigationDispatcher
) : ViewModel() {

    val navigationManager = EnhancedNavigationManager(
        savedStateHandle = savedStateHandle,
        scope = viewModelScope
    )

    fun attachNavController(navController: NavHostController) {
        navigationDispatcher.attach(navController)
    }
}
