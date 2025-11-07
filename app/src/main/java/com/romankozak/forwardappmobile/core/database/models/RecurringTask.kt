package com.romankozak.forwardappmobile.core.database.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_tasks", indices = [androidx.room.Index(value = ["goalId"])])
data class RecurringTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val goalId: String? = null,
    val duration: Int?,
    val priority: TaskPriority,
    val points: Int = 0,

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