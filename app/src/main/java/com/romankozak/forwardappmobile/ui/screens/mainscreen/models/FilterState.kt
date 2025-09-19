package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import com.romankozak.forwardappmobile.data.database.models.Project

data class FilterState(
    val flatList: List<Project>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState
)

