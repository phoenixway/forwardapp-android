package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model

import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority

enum class RecurrenceFrequency {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: List<String>? = null,
)

data class RecurringTask(
    val id: String,
    val title: String,
    val description: String?,
    val goalId: String?,
    val durationMinutes: Int?,
    val priority: TaskPriority,
    val points: Int,
    val recurrenceRule: RecurrenceRule,
    val startDate: Long,
    val endDate: Long?,
)
