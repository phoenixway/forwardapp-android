package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import kotlinx.coroutines.flow.Flow

interface PlanningSettingsProvider {
  val showPlanningModesFlow: Flow<Boolean>
  val dailyTagFlow: Flow<String>
  val mediumTagFlow: Flow<String>
  val longTagFlow: Flow<String>
}
