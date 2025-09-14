//app/src/main/java/com/romankozak/forwardappmobile/data/database/models/DayManagementEntities.kt
package com.romankozak.forwardappmobile.data.database.models

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
    val order: Long? = null // ✅ Added this field for drag-and-drop ordering

)


enum class DayStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    MISSED,
    ARCHIVED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    NONE
}

enum class TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    DEFERRED
}

@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long, // початок дня в millis (midnight UTC)
    val name: String? = null,
    val status: DayStatus = DayStatus.PLANNED,
    val reflection: String? = null,
    val energyLevel: Int? = null, // 1-10 scale
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
    foreignKeys = [
        ForeignKey(
            entity = DayPlan::class,
            parentColumns = ["id"],
            childColumns = ["dayPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GoalList::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ActivityRecord::class,
            parentColumns = ["id"],
            childColumns = ["activityRecordId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dayPlanId"),
        Index("goalId"),
        Index("projectId"),
        Index("activityRecordId"),
        Index("scheduledTime")
    ]
)
data class DayTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val title: String,
    val description: String? = null,

    // Зв'язки з існуючими сутностями
    val goalId: String? = null,
    val projectId: String? = null, // GoalList ID
    val activityRecordId: String? = null,

    // Типізація задачі
    val taskType: ListItemType? = null, // GOAL, SUBLIST, тощо
    val entityId: String? = null, // ID цілі, проекту або іншої сутності

    // Планування та організація
    val order: Long = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val completed: Boolean = false,

    // Часові параметри
    val scheduledTime: Long? = null, // запланований час початку
    val estimatedDurationMinutes: Long? = null,
    val actualDurationMinutes: Long? = null,
    val reminderTime: Long? = null,

    val dueTime: Long? = null, // <--- ДОДАЙТЕ ЦЕЙ РЯДОК


    // Оцінювання (наслідування з Goal)
    @ColumnInfo(defaultValue = "0.0") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val risk: Float = 0f,

    // Контекст
    val location: String? = null,
    val tags: List<String>? = null,
    val notes: String? = null,

    // Метадані
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val completedAt: Long? = null
)

// Додаткова сутність для аналітики
@Entity(tableName = "daily_metrics")
data class DailyMetric(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val date: Long,

    // Продуктивність
    val tasksPlanned: Int = 0,
    val tasksCompleted: Int = 0,
    val completionRate: Float = 0f,

    // Час
    val totalPlannedTime: Long = 0, // хвилини
    val totalActiveTime: Long = 0,
    val totalBreakTime: Long = 0,

    // Енергія та самопочуття
    val morningEnergyLevel: Int? = null,
    val eveningEnergyLevel: Int? = null,
    val overallMood: String? = null,
    val stressLevel: Int? = null, // 1-10

    // Кастомні метрики
    val customMetrics: Map<String, Float>? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
)

// Конвертери для нових типів
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
}

