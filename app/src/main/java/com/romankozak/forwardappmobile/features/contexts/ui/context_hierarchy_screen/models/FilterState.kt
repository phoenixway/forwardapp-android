package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

data class FilterState(
    val flatList: List<HierarchyContextPresentationNode>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState,
    val isReady: Boolean = false,
)
