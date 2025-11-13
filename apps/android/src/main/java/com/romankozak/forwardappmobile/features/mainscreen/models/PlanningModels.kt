package com.romankozak.forwardappmobile.features.mainscreen.models

enum class PlanningMode {
    All,
    Today,
    Medium,
    Long
}

data class PlanningSettingsState(
    val showModes: Boolean = false,
    val dailyTag: String = "",
    val mediumTag: String = "",
    val longTag: String = "",
)
