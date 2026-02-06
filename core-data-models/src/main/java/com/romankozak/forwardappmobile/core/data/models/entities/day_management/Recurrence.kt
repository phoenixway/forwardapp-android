package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import com.google.gson.annotations.SerializedName
import java.time.DayOfWeek

enum class RecurrenceFrequency {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class RecurrenceRule(
    @SerializedName("frequency") val frequency: RecurrenceFrequency,
    @SerializedName("interval") val interval: Int = 1,
    @SerializedName("daysOfWeek") val daysOfWeek: List<DayOfWeek>? = null,
)
