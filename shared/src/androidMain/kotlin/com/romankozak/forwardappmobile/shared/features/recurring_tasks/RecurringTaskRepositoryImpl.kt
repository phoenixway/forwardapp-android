package com.romankozak.forwardappmobile.shared.features.recurring_tasks

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.RecurringTask
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecurringTaskRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : RecurringTaskRepository {

    private val queries = db.recurringTasksQueries

    override fun getRecurringTasks(): Flow<List<RecurringTask>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { tasks -> tasks.map { it.toDomain() } }
    }

    override fun getRecurringTask(id: String): Flow<RecurringTask?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun addRecurringTask(task: RecurringTask) {
        withContext(ioDispatcher) {
            queries.insert(
                id = task.id,
                title = task.title,
                description = task.description,
                goalId = task.goalId,
                duration = task.duration?.toLong(),
                priority = task.priority,
                points = task.points.toLong(),
                frequency = task.recurrenceRule.frequency,
                interval = task.recurrenceRule.interval.toLong(),
                daysOfWeek = task.recurrenceRule.daysOfWeek?.joinToString(","),
                startDate = task.startDate,
                endDate = task.endDate
            )
        }
    }

    override suspend fun deleteRecurringTask(id: String) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }
}