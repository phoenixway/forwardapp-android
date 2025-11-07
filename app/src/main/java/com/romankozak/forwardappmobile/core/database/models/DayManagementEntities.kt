
package com.romankozak.forwardappmobile.core.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class NewTaskParameters(
    val dayPlanId: String,
    val title: String,
    val description: String? = null,
    val goalId: String? = null,
    val projectId: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val scheduledTime: Long? = null,
    val estimatedDurationMinutes: Long? = null,
    val order: Long? = null,
    val taskType: String? = null,
    val points: Int = 0,
)



@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val name: String? = null,
    val status: DayStatus = DayStatus.PLANNED,
    val reflection: String? = null,
    val energyLevel: Int? = null,
    val mood: String? = null,
    val weatherConditions: String? = null,
    val totalPlannedMinutes: Long = 0,
    val totalCompletedMinutes: Long = 0,
    val completionPercentage: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
)

@Entity(
    tableName = "day_tasks",
    indices = [
        Index("dayPlanId"),
        Index("goalId"),
        Index("projectId"),
        Index("activityRecordId"),
        Index("scheduledTime"),
        Index("recurringTaskId"),
    ],
)
data class DayTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val title: String,
    val description: String? = null,
    
    val goalId: String? = null,
    val projectId: String? = null,
    val activityRecordId: String? = null,
    val recurringTaskId: String? = null,
    
    val taskType: String? = null,
    val entityId: String? = null,
    
    val order: Long = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val completed: Boolean = false,
    
    val scheduledTime: Long? = null,
    val estimatedDurationMinutes: Long? = null,
    val actualDurationMinutes: Long? = null,
    val dueTime: Long? = null,
    
    @ColumnInfo(defaultValue = "0.0") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val risk: Float = 0f,
    
    val location: String? = null,
    val tags: List<String>? = null,
    val notes: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val completedAt: Long? = null,
    val nextOccurrenceTime: Long? = null,
    @ColumnInfo(defaultValue = "0") val points: Int = 0,
)


@Entity(tableName = "daily_metrics")
data class DailyMetric(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val date: Long,
    
    val tasksPlanned: Int = 0,
    val tasksCompleted: Int = 0,
    val completionRate: Float = 0f,
    
    val totalPlannedTime: Long = 0,
    val totalActiveTime: Long = 0,
    @ColumnInfo(defaultValue = "0") val completedPoints: Int = 0,
    val totalBreakTime: Long = 0,
    
    val morningEnergyLevel: Int? = null,
    val eveningEnergyLevel: Int? = null,
    val overallMood: String? = null,
    val stressLevel: Int? = null,
    
    val customMetrics: Map<String, Float>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
)


class DailyPlanConverters {
    @TypeConverter
    fun fromDayStatus(status: DayStatus?): String? = status?.name

    @TypeConverter
    fun toDayStatus(value: String?): DayStatus? = value?.let { DayStatus.valueOf(it) }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String? = priority?.name

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority? = value?.let { TaskPriority.valueOf(it) }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String? = status?.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? = value?.let { TaskStatus.valueOf(it) }

    @TypeConverter
    fun fromCustomMetrics(metrics: Map<String, Float>?): String? {
        if (metrics == null) return null
        return Gson().toJson(metrics)
    }

    @TypeConverter
    fun toCustomMetrics(json: String?): Map<String, Float>? {
        if (json == null) return null
        val type = object : TypeToken<Map<String, Float>>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun fromDayOfWeekList(days: List<java.time.DayOfWeek>?): String? {
        return days?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekList(data: String?): List<java.time.DayOfWeek>? {
        return data?.split(",")?.map { java.time.DayOfWeek.valueOf(it) }
    }
}
