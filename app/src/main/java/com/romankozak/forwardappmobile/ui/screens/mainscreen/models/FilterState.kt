package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

data class FilterState(
    val flatList: List<com.romankozak.forwardappmobile.data.database.models.Project>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState,
)
