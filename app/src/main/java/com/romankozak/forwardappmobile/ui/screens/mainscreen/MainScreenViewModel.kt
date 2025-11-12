package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.lifecycle.ViewModel
import me.tatarka.inject.annotations.Inject
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
@Inject
class MainScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    fun onEvent(event: MainScreenEvent) {
        // Handle events if necessary
    }
}
