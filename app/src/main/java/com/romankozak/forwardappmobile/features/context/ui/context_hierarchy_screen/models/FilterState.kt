package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.data.database.models.Project

data class FilterState(
    val flatList: List<Project>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState,
    val isReady: Boolean = false,
)
