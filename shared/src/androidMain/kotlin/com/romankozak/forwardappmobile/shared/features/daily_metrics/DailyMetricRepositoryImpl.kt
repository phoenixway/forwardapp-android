package com.romankozak.forwardappmobile.shared.features.daily_metrics

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.romankozak.forwardappmobile.shared.features.daily_metrics.toDomain

class DailyMetricRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DailyMetricRepository {

    private val queries = db.dailyMetricsQueries

    override fun getDailyMetrics(): Flow<List<DailyMetric>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { metrics -> metrics.map { it.toDomain() } }
    }

    override fun getDailyMetric(id: String): Flow<DailyMetric?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override fun getDailyMetricsForDayPlan(dayPlanId: String): Flow<List<DailyMetric>> {
        return queries.selectByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { metrics -> metrics.map { it.toDomain() } }
    }

    override suspend fun addDailyMetric(metric: DailyMetric) {
        withContext(ioDispatcher) {
            queries.insert(
                id = metric.id,
                dayPlanId = metric.dayPlanId,
                date = metric.date,
                tasksPlanned = metric.tasksPlanned.toLong(),
                tasksCompleted = metric.tasksCompleted.toLong(),
                completionRate = metric.completionRate.toDouble(),
                totalPlannedTime = metric.totalPlannedTime,
                totalActiveTime = metric.totalActiveTime,
                completedPoints = metric.completedPoints.toLong(),
                totalBreakTime = metric.totalBreakTime,
                morningEnergyLevel = metric.morningEnergyLevel?.toLong(),
                eveningEnergyLevel = metric.eveningEnergyLevel?.toLong(),
                overallMood = metric.overallMood,
                stressLevel = metric.stressLevel?.toLong(),
                customMetrics = metric.customMetrics?.let { Json.encodeToString(it) },
                createdAt = metric.createdAt,
                updatedAt = metric.updatedAt
            )
        }
    }

    override suspend fun deleteDailyMetric(id: String) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }
}