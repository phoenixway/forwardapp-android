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
    @SerializedName("linkedAttachmentIds") val linkedAttachmentIds: List<String>?,
    @SerializedName("order") val order: Long = 0L,
    @SerializedName("missionStreamId") val missionStreamId: String? = null,
    @SerializedName("weekKey") val weekKey: String = "",
    @SerializedName("iterationId") val iterationId: String? = null,
    @SerializedName("carriedFromMissionId") val carriedFromMissionId: Long? = null,
    @SerializedName("orderInWeek") val orderInWeek: Long = order,
    @SerializedName("orderInSlot") val orderInSlot: Long? = null,
    @SerializedName("activitySlotContextId") val activitySlotContextId: String? = null,
    @SerializedName("sourceType") val sourceType: String = "MANUAL",
    @SerializedName("sourceContextId") val sourceContextId: String? = null,
    @SerializedName("sourceBacklogItemId") val sourceBacklogItemId: String? = null,
    @SerializedName("sourceArcQuestId") val sourceArcQuestId: String? = null,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0L,
)
