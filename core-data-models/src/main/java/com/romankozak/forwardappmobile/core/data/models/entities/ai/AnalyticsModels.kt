package com.romankozak.forwardappmobile.core.data.models.entities.ai

import com.google.gson.annotations.SerializedName // Add this import
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan

data class DailyAnalytics(
    @SerializedName("dayPlan") val dayPlan: DayPlan,
    @SerializedName("metric") val metric: DailyMetric?,
    @SerializedName("completionRate") val completionRate: Float,
    @SerializedName("totalTimeSpent") val totalTimeSpent: Long,
)

data class WeeklyInsights(
    @SerializedName("totalDays") val totalDays: Int,
    @SerializedName("averageCompletionRate") val averageCompletionRate: Float,
    @SerializedName("totalActiveTime") val totalActiveTime: Long,
    @SerializedName("averageTasksPerDay") val averageTasksPerDay: Float,
    @SerializedName("bestDay") val bestDay: DailyMetric?,
    @SerializedName("worstDay") val worstDay: DailyMetric?,
    @SerializedName("totalTasks") val totalTasks: Int,
    @SerializedName("completedTasks") val completedTasks: Int,
)
