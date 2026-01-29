package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.Context

data class FilterState(
    val flatList: List<Context>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState,
    val isReady: Boolean = false,
)
