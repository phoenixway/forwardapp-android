package com.romankozak.forwardappmobile.config

enum class FeatureFlag(val storageKey: String) {
    AttachmentsLibrary("attachments_library"),
    AllowSystemProjectMoves("allow_system_project_moves"),
    PlanningModes("planning_modes"),
    WifiSync("wifi_sync"),
    StrategicManagement("strategic_management"),
}
