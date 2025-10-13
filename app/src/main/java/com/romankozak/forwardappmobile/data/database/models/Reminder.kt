package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders", primaryKeys = ["id", "goalId"])
data class Reminder(
    val id: String,
    val goalId: String,
    val reminderTime: Long,
    val status: String
)
