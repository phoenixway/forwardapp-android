package com.romankozak.forwardappmobile.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    lateinit var navigationManager: EnhancedNavigationManager
        private set

    // --- ЗМІНА: NavController більше не потрібен для ініціалізації ---
    fun initialize() {
        if (!::navigationManager.isInitialized) {
            // Передаємо лише savedStateHandle та scope
            navigationManager = EnhancedNavigationManager(savedStateHandle, viewModelScope)
        }
    }
}