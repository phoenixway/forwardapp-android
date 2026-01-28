package com.romankozak.forwardappmobile.sync

data class SettingsContent(
    val settings: Map<String, String>,
)

data class RecentProjectEntry(
    val contextId: String,
    val timestamp: Long,
)
