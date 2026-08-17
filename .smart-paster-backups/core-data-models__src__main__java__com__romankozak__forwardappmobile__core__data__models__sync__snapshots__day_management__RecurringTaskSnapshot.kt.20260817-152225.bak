package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName

data class RecurringTaskSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("goalId") val goalId: String?,
    @SerializedName("linkedProjectIds") val linkedProjectIds: List<String>?,
    @SerializedName("linkedAttachmentIds") val linkedAttachmentIds: List<String>?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("priority") val priority: String,
    @SerializedName("points") val points: Int,
    @SerializedName("recurrenceRule") val recurrenceRule: RecurrenceRuleSnapshot,
    @SerializedName("startDate") val startDate: Long,
    @SerializedName("endDate") val endDate: Long?,
)
