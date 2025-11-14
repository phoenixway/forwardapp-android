package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DayTaskRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : DayTaskRepository {

    override fun observeTasksForPlan(dayPlanId: String): Flow<List<DayTask>> =
        database.dayTasksQueries.getTasksForDayPlan(dayPlanId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTaskById(taskId: String): DayTask? = withContext(dispatcher) {
        database.dayTasksQueries.getTaskById(taskId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertTask(task: DayTask) = withContext(dispatcher) {
        database.dayTasksQueries.insertDayTask(
            id = task.id,
            dayPlanId = task.dayPlanId,
            title = task.title,
            description = task.description,
            goalId = task.goalId,
            projectId = task.projectId,
            activityRecordId = task.activityRecordId,
            recurringTaskId = task.recurringTaskId,
            taskType = task.taskType,
            entityId = task.entityId,
            order = task.order,
            priority = task.priority,
            status = task.status,
            completed = task.completed,
            scheduledTime = task.scheduledTime,
            estimatedDurationMinutes = task.estimatedDurationMinutes,
            actualDurationMinutes = task.actualDurationMinutes,
            dueTime = task.dueTime,
            valueImportance = task.valueImportance.toDouble(),
            valueImpact = task.valueImpact.toDouble(),
            effort = task.effort.toDouble(),
            cost = task.cost.toDouble(),
            risk = task.risk.toDouble(),
            location = task.location,
            tags = task.tags,
            notes = task.notes,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            completedAt = task.completedAt,
            nextOccurrenceTime = task.nextOccurrenceTime,
            points = task.points,
        )
    }

    override suspend fun deleteTask(taskId: String) = withContext(dispatcher) {
        database.dayTasksQueries.deleteDayTask(taskId)
    }

    override suspend fun deleteTasksForPlan(dayPlanId: String) = withContext(dispatcher) {
        database.dayTasksQueries.deleteTasksForPlan(dayPlanId)
    }
}
