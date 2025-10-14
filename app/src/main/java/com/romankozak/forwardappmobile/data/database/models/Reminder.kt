package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityId: String,      // ID of the entity (Goal, Project, Task)
    val entityType: String,    // "GOAL", "PROJECT", "TASK"
    val reminderTime: Long,
    val status: String,        // "SCHEDULED", "COMPLETED", "SNOOZED", "DISMISSED"
    val creationTime: Long,
    val snoozeUntil: Long? = null
)