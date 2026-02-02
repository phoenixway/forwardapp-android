package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import java.time.DayOfWeek

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
    val daysOfWeek: List<DayOfWeek>? = null,
)
