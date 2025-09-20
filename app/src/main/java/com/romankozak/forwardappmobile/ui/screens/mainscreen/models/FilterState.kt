package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState

data class FilterState(
    val flatList: List<com.romankozak.forwardappmobile.data.database.models.Project>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState
)