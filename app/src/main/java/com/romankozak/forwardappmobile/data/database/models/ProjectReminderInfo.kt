package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_reminder_info")
data class ProjectReminderInfo(
    @PrimaryKey val projectId: String,
    @ColumnInfo(name = "reminder_status") val reminderStatus: String = "ACTIVE",
    @ColumnInfo(name = "snooze_time") val snoozeTime: Long? = null,
)
