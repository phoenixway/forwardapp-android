package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.model.DailyMetric
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.repository.DailyMetricsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DailyMetricsRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : DailyMetricsRepository {

    override fun observeMetrics(): Flow<List<DailyMetric>> =
        database.dailyMetricsQueries.getDailyMetrics()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeMetricsForDayPlan(dayPlanId: String): Flow<List<DailyMetric>> =
        database.dailyMetricsQueries.getMetricsForDayPlan(dayPlanId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getMetricById(id: String): DailyMetric? = withContext(dispatcher) {
        database.dailyMetricsQueries.getMetricById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertMetric(metric: DailyMetric) = withContext(dispatcher) {
        database.dailyMetricsQueries.insertDailyMetric(
            id = metric.id,
            dayPlanId = metric.dayPlanId,
            date = metric.date,
            tasksPlanned = metric.tasksPlanned,
            tasksCompleted = metric.tasksCompleted,
            completionRate = metric.completionRate,
            totalPlannedTime = metric.totalPlannedTime,
            totalActiveTime = metric.totalActiveTime,
            completedPoints = metric.completedPoints,
            totalBreakTime = metric.totalBreakTime,
            morningEnergyLevel = metric.morningEnergyLevel,
            eveningEnergyLevel = metric.eveningEnergyLevel,
            overallMood = metric.overallMood,
            stressLevel = metric.stressLevel,
            customMetrics = metric.customMetrics,
            createdAt = metric.createdAt,
            updatedAt = metric.updatedAt,
        )
    }

    override suspend fun deleteMetric(id: String) = withContext(dispatcher) {
        database.dailyMetricsQueries.deleteDailyMetric(id)
    }
}
