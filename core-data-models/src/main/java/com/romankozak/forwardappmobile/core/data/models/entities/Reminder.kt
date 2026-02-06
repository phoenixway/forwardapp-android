package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("entityId") val entityId: String, // ID of the entity (Goal, Project, Task)
    @SerializedName("entityType") val entityType: String, // "GOAL", "PROJECT", "TASK"
    @SerializedName("reminderTime") val reminderTime: Long,
    @SerializedName("status") val status: String, // "SCHEDULED", "COMPLETED", "SNOOZED", "DISMISSED"
    @SerializedName("creationTime") val creationTime: Long,
    @SerializedName("snoozeUntil") val snoozeUntil: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
)