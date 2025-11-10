package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class MainScreenViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    fun onEvent(event: MainScreenEvent) {
        // Handle events if necessary
    }
}
