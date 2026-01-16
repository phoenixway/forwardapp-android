package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import kotlinx.coroutines.flow.StateFlow

interface PlanningSearchAdapter {
  val searchQuery: StateFlow<TextFieldValue>
  val subStateStack: StateFlow<List<MainSubState>>

  fun isSearchActive(): Boolean
  fun popToSubState(targetState: MainSubState)
  fun onToggleSearch(isActive: Boolean)
}
