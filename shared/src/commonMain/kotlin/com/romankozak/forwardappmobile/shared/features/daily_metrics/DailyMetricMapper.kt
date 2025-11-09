package com.romankozak.forwardappmobile.shared.features.daily_metrics

import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.shared.database.Daily_metrics
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun Daily_metrics.toDomain(): DailyMetric {
    return DailyMetric(
        id = this.id,
        dayPlanId = this.day_plan_id,
        date = this.date,
        tasksPlanned = this.tasks_planned.toInt(),
        tasksCompleted = this.tasks_completed.toInt(),
        completionRate = this.completion_rate.toFloat(),
        totalPlannedTime = this.total_planned_time,
        totalActiveTime = this.total_active_time,
        completedPoints = this.completed_points.toInt(),
        totalBreakTime = this.total_break_time,
        morningEnergyLevel = this.morning_energy_level?.toInt(),
        eveningEnergyLevel = this.evening_energy_level?.toInt(),
        overallMood = this.overall_mood,
        stressLevel = this.stress_level?.toInt(),
        customMetrics = this.custom_metrics?.let { Json.decodeFromString(it) },
        createdAt = this.created_at,
        updatedAt = this.updated_at
    )
}
