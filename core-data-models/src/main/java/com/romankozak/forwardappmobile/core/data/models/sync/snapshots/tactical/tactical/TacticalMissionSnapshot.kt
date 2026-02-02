package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical

import com.google.gson.annotations.SerializedName

data class TacticalMissionSnapshot(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("startTime") val startTime: Long?,
    @SerializedName("deadline") val deadline: Long,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("projectId") val projectId: String?,
    @SerializedName("linkedProjectIds") val linkedProjectIds: List<String>?,
    @SerializedName("linkedAttachmentIds") val linkedAttachmentIds: List<String>?
)
