package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc

import com.google.gson.annotations.SerializedName

data class FocusContextIntervalSnapshot(
    @SerializedName("id") val id: Long,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("scope") val scope: String,
    @SerializedName("priority") val priority: Int? = null,
    @SerializedName("source") val source: String,
    @SerializedName("createdFromActivityId") val createdFromActivityId: String? = null,
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("endedAt") val endedAt: Long? = null,
)
