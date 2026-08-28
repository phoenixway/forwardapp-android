package com.romankozak.forwardappmobile.features.activitytracker.reflection

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink

enum class ReflectionPeriod(
    val title: String,
    val operationalDayCount: Int,
) {
    DAY("День", 1),
    THREE_DAYS("3 дні", 3),
    WEEK("Тиждень", 7),
}

data class TagTimeStat(
    val tag: String,
    val durationMillis: Long,
    val share: Float,
)

data class EntityTimeStat(
    val link: ActivityEntityLink,
    val title: String,
    val durationMillis: Long,
    val trackedDayCount: Int,
    val share: Float,
)

data class TimeReflection(
    val period: ReflectionPeriod,
    val rangeStart: Long?,
    val rangeEnd: Long,
    val recordedDayCount: Int,
    val totalTrackedMillis: Long,
    val tagStats: List<TagTimeStat>,
    val entityStats: List<EntityTimeStat> = emptyList(),
)

data class TimeReflectionUiState(
    val reflection: TimeReflection =
        TimeReflection(
            period = ReflectionPeriod.DAY,
            rangeStart = null,
            rangeEnd = System.currentTimeMillis(),
            recordedDayCount = 0,
            totalTrackedMillis = 0,
            tagStats = emptyList(),
            entityStats = emptyList(),
        ),
    val isLoading: Boolean = true,
    val availableDayStarts: List<Long> = emptyList(),
    val selectedDayStart: Long? = null,
    val hasPreviousDay: Boolean = false,
    val hasNextDay: Boolean = false,
    val isLatestDay: Boolean = true,
)
