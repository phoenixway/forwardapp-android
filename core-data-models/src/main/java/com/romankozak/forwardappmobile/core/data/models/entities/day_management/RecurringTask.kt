package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import java.util.UUID

@Entity(tableName = "recurring_tasks", indices = [Index(value = ["goalId"])])
data class RecurringTask(
    @PrimaryKey @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("goalId")
    val goalId: String? = null,
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("priority")
    val priority: TaskPriority,
    @SerializedName("points")
    val points: Int = 0,
    @Embedded @SerializedName("recurrenceRule")
    val recurrenceRule: RecurrenceRule,
    @SerializedName("startDate")
    val startDate: Long, // Timestamp
    @SerializedName("endDate")
    val endDate: Long? = null, // Timestamp
)

@Fts4(contentEntity = RecurringTask::class)
@Entity(tableName = "recurring_tasks_fts")
data class RecurringTaskFts(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
)
