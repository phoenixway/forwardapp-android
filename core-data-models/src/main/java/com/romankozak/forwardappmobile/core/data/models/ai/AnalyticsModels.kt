package com.romankozak.forwardappmobile.core.data.models.ai

import com.romankozak.forwardappmobile.core.data.models.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.day_management.DayPlan

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
