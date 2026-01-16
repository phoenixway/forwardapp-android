package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.MainSubState
import kotlinx.coroutines.flow.StateFlow

interface PlanningSearchAdapter {
  val searchQuery: StateFlow<TextFieldValue>
  val subStateStack: StateFlow<List<MainSubState>>

  fun isSearchActive(): Boolean
  fun popToSubState(targetState: MainSubState)
  fun onToggleSearch(isActive: Boolean)
}
