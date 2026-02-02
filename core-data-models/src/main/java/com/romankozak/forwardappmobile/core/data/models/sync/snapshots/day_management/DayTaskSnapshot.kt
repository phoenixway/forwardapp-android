package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName

data class DayTaskSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("goalId") val goalId: String?,
    @SerializedName("projectId") val projectId: String?,
    @SerializedName("activityRecordId") val activityRecordId: String?,
    @SerializedName("recurringTaskId") val recurringTaskId: String?,
    @SerializedName("taskType") val taskType: String?,
    @SerializedName("entityId") val entityId: String?,
    @SerializedName("order") val order: Long,
    @SerializedName("priority") val priority: String,
    @SerializedName("status") val status: String,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("scheduledTime") val scheduledTime: Long?,
    @SerializedName("estimatedDurationMinutes") val estimatedDurationMinutes: Long?,
    @SerializedName("actualDurationMinutes") val actualDurationMinutes: Long?,
    @SerializedName("dueTime") val dueTime: Long?,
    @SerializedName("valueImportance") val valueImportance: Float,
    @SerializedName("valueImpact") val valueImpact: Float,
    @SerializedName("effort") val effort: Float,
    @SerializedName("cost") val cost: Float,
    @SerializedName("risk") val risk: Float,
    @SerializedName("location") val location: String?,
    @SerializedName("tags") val tags: List<String>?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
    @SerializedName("completedAt") val completedAt: Long?,
    @SerializedName("nextOccurrenceTime") val nextOccurrenceTime: Long?,
    @SerializedName("points") val points: Int
)
