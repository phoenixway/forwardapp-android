package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.domain.DayTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.toDayTaskDomain

class DayTaskRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DayTaskRepository {

    private val queries = db.dayTasksQueries

    override fun getDayTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>> {
        return queries.selectAllByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks: List<com.romankozak.forwardappmobile.shared.database.DayTasks> -> dayTasks.map { it.toDayTaskDomain() } }
    }

    override fun getDayTaskById(id: String): Flow<DayTask?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDayTaskDomain() }
    }

    override suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long {
        return withContext(ioDispatcher) {
            queries.getMaxOrderForDayPlan(dayPlanId).executeAsOneOrNull()?.MAX ?: 0L
        }
    }

    override fun getTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>> {
        return queries.selectAllByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks: List<com.romankozak.forwardappmobile.shared.database.DayTasks> -> dayTasks.map { it.toDayTaskDomain() } }
    }

    override fun getTasksForGoal(goalId: String): Flow<List<DayTask>> {
        return queries.selectTasksForGoal(goalId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks: List<com.romankozak.forwardappmobile.shared.database.DayTasks> -> dayTasks.map { it.toDayTaskDomain() } }
    }

    override suspend fun getTasksForDayPlanOnce(dayPlanId: String): List<DayTask> {
        return withContext(ioDispatcher) {
            queries.selectAllByDayPlanId(dayPlanId).executeAsList().map { it.toDayTaskDomain() }
        }
    }

    override suspend fun updateTaskOrder(taskId: String, newOrder: Long, updatedAt: Long) {
        withContext(ioDispatcher) {
            queries.updateTaskOrder(
                taskId = taskId,
                newOrder = newOrder,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun updateTaskCompletion(
        taskId: String,
        completed: Boolean,
        status: TaskStatus,
        completedAt: Long?,
        updatedAt: Long
    ) {
        withContext(ioDispatcher) {
            queries.updateTaskCompletion(
                taskId = taskId,
                completed = completed,
                status = status,
                completedAt = completedAt,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun deleteTasksForDayPlanIds(recurringTaskId: String, dayPlanIds: List<String>) {
        withContext(ioDispatcher) {
            queries.deleteTasksForDayPlanIds(
                recurringTaskId = recurringTaskId,
                dayPlanIds = dayPlanIds
            )
        }
    }

    override suspend fun linkTaskWithActivity(taskId: String, activityRecordId: String, updatedAt: Long) {
        withContext(ioDispatcher) {
            queries.linkTaskWithActivity(
                taskId = taskId,
                activityRecordId = activityRecordId,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun updateTaskDuration(taskId: String, durationMinutes: Long, updatedAt: Long) {
        withContext(ioDispatcher) {
            queries.updateTaskDuration(
                taskId = taskId,
                durationMinutes = durationMinutes,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun findByRecurringIdAndDayPlanId(recurringTaskId: String, dayPlanId: String): DayTask? {
        return withContext(ioDispatcher) {
            queries.selectByRecurringIdAndDayPlanId(recurringTaskId, dayPlanId).executeAsOneOrNull()?.toDayTaskDomain()
        }
    }

    override suspend fun findTemplateForRecurringTask(recurringTaskId: String): DayTask? {
        return withContext(ioDispatcher) {
            queries.selectTemplateForRecurringTask(recurringTaskId).executeAsOneOrNull()?.toDayTaskDomain()
        }
    }

    override suspend fun detachFromRecurrence(taskId: String) {
        withContext(ioDispatcher) {
            queries.detachFromRecurrence(
                taskId = taskId,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override suspend fun updateNextOccurrenceTime(taskId: String, nextOccurrenceTime: Long) {
        withContext(ioDispatcher) {
            queries.updateNextOccurrenceTime(
                taskId = taskId,
                nextOccurrenceTime = nextOccurrenceTime,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override suspend fun insertDayTask(dayTask: DayTask) {
        withContext(ioDispatcher) {
            queries.insert(
                id = dayTask.id,
                dayPlanId = dayTask.dayPlanId,
                title = dayTask.title,
                description = dayTask.description,
                goalId = dayTask.goalId,
                projectId = dayTask.projectId,
                activityRecordId = dayTask.activityRecordId,
                recurringTaskId = dayTask.recurringTaskId,
                taskType = dayTask.taskType,
                entityId = dayTask.entityId,
                order = dayTask.order,
                priority = dayTask.priority,
                status = dayTask.status,
                completed = dayTask.completed,
                scheduledTime = dayTask.scheduledTime,
                estimatedDurationMinutes = dayTask.estimatedDurationMinutes,
                actualDurationMinutes = dayTask.actualDurationMinutes,
                dueTime = dayTask.dueTime,
                valueImportance = dayTask.valueImportance,
                valueImpact = dayTask.valueImpact,
                effort = dayTask.effort,
                cost = dayTask.cost,
                risk = dayTask.risk,
                location = dayTask.location,
                tags = dayTask.tags,
                notes = dayTask.notes,
                createdAt = dayTask.createdAt,
                updatedAt = dayTask.updatedAt,
                completedAt = dayTask.completedAt,
                nextOccurrenceTime = dayTask.nextOccurrenceTime,
                points = dayTask.points
            )
        }
    }

    override suspend fun updateDayTask(dayTask: DayTask) {
        withContext(ioDispatcher) {
            queries.update(
                id = dayTask.id,
                dayPlanId = dayTask.dayPlanId,
                title = dayTask.title,
                description = dayTask.description,
                goalId = dayTask.goalId,
                projectId = dayTask.projectId,
                activityRecordId = dayTask.activityRecordId,
                recurringTaskId = dayTask.recurringTaskId,
                taskType = dayTask.taskType,
                entityId = dayTask.entityId,
                order = dayTask.order,
                priority = dayTask.priority,
                status = dayTask.status,
                completed = dayTask.completed,
                scheduledTime = dayTask.scheduledTime,
                estimatedDurationMinutes = dayTask.estimatedDurationMinutes,
                actualDurationMinutes = dayTask.actualDurationMinutes,
                dueTime = dayTask.dueTime,
                valueImportance = dayTask.valueImportance,
                valueImpact = dayTask.valueImpact,
                effort = dayTask.effort,
                cost = dayTask.cost,
                risk = dayTask.risk,
                location = dayTask.location,
                tags = dayTask.tags,
                notes = dayTask.notes,
                updatedAt = dayTask.updatedAt,
                completedAt = dayTask.completedAt,
                nextOccurrenceTime = dayTask.nextOccurrenceTime,
                points = dayTask.points
            )
        }
    }

    override suspend fun deleteDayTask(id: String) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }

    override suspend fun deleteAllDayTasksForDayPlan(dayPlanId: String) {
        withContext(ioDispatcher) {
            queries.deleteAllByDayPlanId(dayPlanId)
        }
    }
}
