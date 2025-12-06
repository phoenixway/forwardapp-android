

package com.romankozak.forwardappmobile.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        lateinit var navigationManager: EnhancedNavigationManager
            private set

        fun initialize() {
            if (!::navigationManager.isInitialized) {
                navigationManager = EnhancedNavigationManager(savedStateHandle, viewModelScope)
                
                
                navigationManager.navigateToProjectHierarchyScreen(isInitial = true)
            }
        }
    }
