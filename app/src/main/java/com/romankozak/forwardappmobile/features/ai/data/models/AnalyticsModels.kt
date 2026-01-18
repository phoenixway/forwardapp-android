package com.romankozak.forwardappmobile.features.ai.data.models

import com.romankozak.forwardappmobile.features.daymanagement.data.models.DailyMetric
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayPlan


data class DailyAnalytics(
    val dayPlan: DayPlan,
    val metric: DailyMetric?,
    val completionRate: Float,
    val totalTimeSpent: Long,
)

data class WeeklyInsights(
    val totalDays: Int,
    val averageCompletionRate: Float,
    val totalActiveTime: Long,
    val averageTasksPerDay: Float,
    val bestDay: DailyMetric?,
    val worstDay: DailyMetric?,
    val totalTasks: Int,
    val completedTasks: Int,
)
