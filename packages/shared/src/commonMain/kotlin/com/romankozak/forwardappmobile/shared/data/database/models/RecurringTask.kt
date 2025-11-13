package com.romankozak.forwardappmobile.shared.data.database.models

// Assuming these enums are also moved to commonMain
enum class TaskPriority { LOW, MEDIUM, HIGH, CRITICAL, NONE }
enum class RecurrenceFrequency { HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY }

data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: List<String>? = null // Using String for KMP compatibility
)

data class RecurringTask(
    val id: String,
    val title: String,
    val description: String?,
    val goalId: String?,
    val duration: Int?,
    val priority: TaskPriority,
    val points: Int,
    val recurrenceRule: RecurrenceRule,
    val startDate: Long,
    val endDate: Long?
)
