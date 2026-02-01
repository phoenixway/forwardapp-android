package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.annotations.SerializedName

data class SettingsContent(
    val settings: Map<String, String>,
)

data class RecentProjectEntry(
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String = "",
    val timestamp: Long,
)
