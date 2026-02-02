package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.reminders

import com.google.gson.annotations.SerializedName

data class ReminderSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("entityType") val entityType: String,
    @SerializedName("reminderTime") val reminderTime: Long,
    @SerializedName("status") val status: String,
    @SerializedName("creationTime") val creationTime: Long,
    @SerializedName("snoozeUntil") val snoozeUntil: Long?,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)
