package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

enum class PlanningMode {
    All,
    Today,
    Medium,
    Long
}

data class PlanningSettingsState(
    val showPlanningModes: Boolean,
    val dailyTag: String,
    val mediumTag: String,
    val longTag: String
)
