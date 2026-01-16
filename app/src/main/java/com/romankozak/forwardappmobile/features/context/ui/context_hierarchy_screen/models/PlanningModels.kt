package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models

enum class ProjectHierarchyScreenPlanningMode {
    All,
    Today,
    Medium,
    Long
}

typealias PlanningMode = ProjectHierarchyScreenPlanningMode

data class PlanningSettingsState(
    val showModes: Boolean = false,
    val dailyTag: String = "",
    val mediumTag: String = "",
    val longTag: String = "",
)
