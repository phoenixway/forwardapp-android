package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.repository

import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.model.DailyMetric
import kotlinx.coroutines.flow.Flow

interface DailyMetricsRepository {
    fun observeMetrics(): Flow<List<DailyMetric>>

    fun observeMetricsForDayPlan(dayPlanId: String): Flow<List<DailyMetric>>

    suspend fun getMetricById(id: String): DailyMetric?

    suspend fun upsertMetric(metric: DailyMetric)

    suspend fun deleteMetric(id: String)
}
