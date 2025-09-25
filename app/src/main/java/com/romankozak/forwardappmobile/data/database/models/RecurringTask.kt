package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_tasks")
data class RecurringTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val duration: Int?,
    val priority: TaskPriority,

    @Embedded
    val recurrenceRule: RecurrenceRule,

    val startDate: Long, // Timestamp
    val endDate: Long? = null // Timestamp
)

@Fts4(contentEntity = RecurringTask::class)
@Entity(tableName = "recurring_tasks_fts")
data class RecurringTaskFts(
    val title: String,
    val description: String?,
)