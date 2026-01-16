package com.romankozak.forwardappmobile.features.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val navigationManager = EnhancedNavigationManager(
        savedStateHandle = savedStateHandle,
        scope = viewModelScope
    )
}
