package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases

import kotlinx.coroutines.flow.Flow

interface PlanningSettingsProvider {
  val showPlanningModesFlow: Flow<Boolean>
  val dailyTagFlow: Flow<String>
  val mediumTagFlow: Flow<String>
  val longTagFlow: Flow<String>
}
