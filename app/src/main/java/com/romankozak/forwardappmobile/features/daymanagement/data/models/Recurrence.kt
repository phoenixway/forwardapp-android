package com.romankozak.forwardappmobile.features.daymanagement.data.models

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
