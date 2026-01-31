package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.activity

import com.google.gson.annotations.SerializedName

data class ActivityRecordSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("startTime") val startTime: Long?,
    @SerializedName("endTime") val endTime: Long?,
    @SerializedName("reminderTime") val reminderTime: Long?,
    @SerializedName("targetId") val targetId: String?,
    @SerializedName("targetType") val targetType: String?,
    @SerializedName("updatedAt") val updatedAt: Long, // All snapshots should have non-nullable updatedAt
    @SerializedName("goalId") val goalId: String?,
    @SerializedName("contextId") val contextId: String?,
    @SerializedName("xpGained") val xpGained: Int?,
    @SerializedName("antyXp") val antyXp: Int?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)
