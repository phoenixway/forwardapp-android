package com.romankozak.forwardappmobile.shared.features.daily_metrics

import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import kotlinx.coroutines.flow.Flow

interface DailyMetricRepository {
    fun getDailyMetrics(): Flow<List<DailyMetric>>
    fun getDailyMetric(id: String): Flow<DailyMetric?>
    fun getDailyMetricsForDayPlan(dayPlanId: String): Flow<List<DailyMetric>>
    suspend fun addDailyMetric(metric: DailyMetric)
    suspend fun deleteDailyMetric(id: String)
}
