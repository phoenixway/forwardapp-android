package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import java.time.DayOfWeek
import java.util.Locale
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
    val dueTime: Long? = null,
    val executionStrictness: TaskExecutionStrictness = TaskExecutionStrictness.NORMAL,
    val order: Long? = null,
    val taskType: String? = null,
    val points: Int = 0,
    val linkedProjectIds: List<String>? = null,
    val linkedAttachmentIds: List<String>? = null,
)

enum class DayFocusType {
    FOCUS,
    RESPONSIBILITY,
}

@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("date") val date: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("linkedProjectIds") val linkedProjectIds: List<String>? = emptyList(),
    @SerializedName("linkedAttachmentIds") val linkedAttachmentIds: List<String>? = emptyList(),
    @SerializedName("status") val status: DayStatus = DayStatus.PLANNED,
    @SerializedName("reflection") val reflection: String? = null,
    @SerializedName("energyLevel") val energyLevel: Int? = null,
    @SerializedName("mood") val mood: String? = null,
    @SerializedName("weatherConditions") val weatherConditions: String? = null,
    @SerializedName("totalPlannedMinutes") val totalPlannedMinutes: Long = 0,
    @SerializedName("totalCompletedMinutes") val totalCompletedMinutes: Long = 0,
    @SerializedName("completionPercentage") val completionPercentage: Float = 0f,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
)

@Entity(
    tableName = "day_tasks",
    foreignKeys = [
        ForeignKey(
            entity = DayPlan::class,
            parentColumns = ["id"],
            childColumns = ["dayPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ActivityRecord::class,
            parentColumns = ["id"],
            childColumns = ["activityRecordId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RecurringTask::class,
            parentColumns = ["id"],
            childColumns = ["recurringTaskId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
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
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("goalId") val goalId: String? = null,
    @SerializedName(value = "projectId", alternate = ["contextId"]) val projectId: String? = null,
    @SerializedName("linkedProjectIds") val linkedProjectIds: List<String>? = emptyList(),
    @SerializedName("linkedAttachmentIds") val linkedAttachmentIds: List<String>? = emptyList(),
    @SerializedName("activityRecordId") val activityRecordId: String? = null,
    @SerializedName("recurringTaskId") val recurringTaskId: String? = null,
    @SerializedName("taskType") val taskType: String? = null,
    @SerializedName("entityId") val entityId: String? = null,
    @SerializedName("order") val order: Long = 0,
    @SerializedName("priority") val priority: TaskPriority = TaskPriority.MEDIUM,
    @SerializedName("status") val status: TaskStatus = TaskStatus.NOT_STARTED,
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("scheduledTime") val scheduledTime: Long? = null,
    @SerializedName("estimatedDurationMinutes") val estimatedDurationMinutes: Long? = null,
    @SerializedName("actualDurationMinutes") val actualDurationMinutes: Long? = null,
    @SerializedName("dueTime") val dueTime: Long? = null,
    @ColumnInfo(defaultValue = "'NORMAL'") @SerializedName("executionStrictness") val executionStrictness: TaskExecutionStrictness = TaskExecutionStrictness.NORMAL,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("valueImportance") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("valueImpact") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("effort") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("cost") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("risk") val risk: Float = 0f,
    @SerializedName("location") val location: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
    @SerializedName("completedAt") val completedAt: Long? = null,
    @SerializedName("nextOccurrenceTime") val nextOccurrenceTime: Long? = null,
    @ColumnInfo(defaultValue = "0") @SerializedName("points") val points: Int = 0,
)

@Entity(
    tableName = "day_focus_items",
    foreignKeys = [
        ForeignKey(
            entity = DayPlan::class,
            parentColumns = ["id"],
            childColumns = ["dayPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dayPlanId"),
        Index(value = ["dayPlanId", "order"]),
    ],
)
data class DayFocusItem(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("title") val title: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("relatedLinks") val relatedLinks: List<RelatedLink>? = emptyList(),
    @SerializedName("type") val type: DayFocusType = DayFocusType.FOCUS,
    @ColumnInfo(defaultValue = "0") @SerializedName("isEveryday") val isEveryday: Boolean = false,
    @SerializedName("recurringKey") val recurringKey: String? = null,
    @SerializedName("order") val order: Long = 0,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
)

@Entity(tableName = "daily_metrics")
data class DailyMetric(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("date") val date: Long,
    @SerializedName("tasksPlanned") val tasksPlanned: Int = 0,
    @SerializedName("tasksCompleted") val tasksCompleted: Int = 0,
    @SerializedName("completionRate") val completionRate: Float = 0f,
    @SerializedName("totalPlannedTime") val totalPlannedTime: Long = 0,
    @SerializedName("totalActiveTime") val totalActiveTime: Long = 0,
    @ColumnInfo(defaultValue = "0") @SerializedName("completedPoints") val completedPoints: Int = 0,
    @SerializedName("totalBreakTime") val totalBreakTime: Long = 0,
    @SerializedName("morningEnergyLevel") val morningEnergyLevel: Int? = null,
    @SerializedName("eveningEnergyLevel") val eveningEnergyLevel: Int? = null,
    @SerializedName("overallMood") val overallMood: String? = null,
    @SerializedName("stressLevel") val stressLevel: Int? = null,
    @SerializedName("customMetrics") val customMetrics: Map<String, Float>? = null,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
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
    fun fromTaskExecutionStrictness(strictness: TaskExecutionStrictness?): String? = strictness?.name

    @TypeConverter
    fun toTaskExecutionStrictness(value: String?): TaskExecutionStrictness? = value?.let { TaskExecutionStrictness.valueOf(it) }

    @TypeConverter
    fun fromDayFocusType(type: DayFocusType?): String? = type?.name

    @TypeConverter
    fun toDayFocusType(value: String?): DayFocusType? = value?.let { DayFocusType.valueOf(it) }

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
    fun fromDayOfWeekList(days: List<DayOfWeek>?): String? {
        return days?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekList(data: String?): List<DayOfWeek>? {
        if (data.isNullOrBlank()) return null

        val parsedDays =
            data
                .split(",")
                .mapNotNull { rawDay ->
                    val normalized = rawDay.trim()
                    if (normalized.isEmpty()) return@mapNotNull null

                    normalized.toIntOrNull()?.let { numericDay ->
                        if (numericDay in 1..7) return@mapNotNull DayOfWeek.of(numericDay)
                    }

                    runCatching { DayOfWeek.valueOf(normalized.uppercase(Locale.ROOT)) }.getOrNull()
                }
                .distinct()

        return parsedDays.ifEmpty { null }
    }
}
