
package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import com.romankozak.forwardappmobile.core.data.models.entities.ai.DailyAnalytics
import com.romankozak.forwardappmobile.core.data.models.entities.ai.WeeklyInsights
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.domain.ai.events.TaskCompletedEvent
import com.romankozak.forwardappmobile.domain.ai.events.TaskCreatedEvent
import com.romankozak.forwardappmobile.domain.ai.events.TaskDeferredEvent
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain.TaskExecutionTimingCalculator
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain.TaskExecutionTimingRequest
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.platform.TaskExecutionAlarmCoordinator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class DayManagementRepository
    @Inject
    constructor(
        private val dayPlanDao: DayPlanDao,
        private val dayFocusItemDao: DayFocusItemDao,
        private val dayTaskDao: DayTaskDao,
        private val dailyMetricDao: DailyMetricDao,
        private val goalDao: com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao,
        private val contextDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao,
        private val canonicalRecurrenceMaterializationAdapter: com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceMaterializationAdapter,
        private val listItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao,
        private val activityRepository: ActivityRepository,
        private val taskExecutionTimingCalculator: TaskExecutionTimingCalculator,
        private val taskExecutionAlarmCoordinator: TaskExecutionAlarmCoordinator,
        private val aiEventRepository: AiEventRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private companion object {
            const val PRIORITY_RANK_CRITICAL = 0
            const val PRIORITY_RANK_HIGH = 1
            const val PRIORITY_RANK_MEDIUM = 2
            const val PRIORITY_RANK_LOW = 3
            const val PRIORITY_RANK_NONE = 4
            const val DAYS_IN_WEEK = 7
            const val HOURS_IN_DAY = 24
            const val MINUTES_IN_HOUR = 60
            const val SECONDS_IN_MINUTE = 60
            const val MILLIS_IN_SECOND = 1000L
            const val MILLIS_IN_MINUTE = 60000L
        }

        data class UpdateTaskParams(
            val taskId: String,
            val title: String,
            val description: String?,
            val priority: TaskPriority,
            val duration: Long?,
            val scheduledTime: Long?,
            val dueTime: Long?,
            val executionStrictness: TaskExecutionStrictness,
            val points: Int,
            val projectId: String? = null,
            val linkedProjectIds: List<String>? = null,
            val updateContextLinks: Boolean = false,
        )

        @Volatile
        private var cachedBestCompletedPoints: Int? = null

        fun getPlanByIdStream(planId: String): Flow<DayPlan?> = dayPlanDao.getPlanByIdStream(planId)

        suspend fun getPlanById(planId: String): DayPlan? =
            withContext(ioDispatcher) {
                dayPlanDao.getPlanById(planId)
            }

        fun getPlanForDate(date: Long): Flow<DayPlan?> = dayPlanDao.getPlanForDate(getDayStart(date))

        suspend fun getPlanIdForDate(date: Long): String? =
            withContext(ioDispatcher) {
                val dayStart = getDayStart(date)
                dayPlanDao.getPlanForDateSync(dayStart)?.id
            }

        suspend fun createOrUpdateDayPlan(
            date: Long,
            name: String? = null,
        ): DayPlan =
            withContext(ioDispatcher) {
                val dayStart = getDayStart(date)
                val existingPlan = dayPlanDao.getPlanForDateSync(dayStart)

                if (existingPlan != null) {
                    val now = System.currentTimeMillis()
                    val updated =
                        existingPlan.copy(
                            name = name ?: existingPlan.name,
                            updatedAt = now,
                            syncedAt = null,
                            version = existingPlan.version + 1,
                        )
                    dayPlanDao.update(updated)
                    updated
                } else {
                    val now = System.currentTimeMillis()
                    val newPlan =
                        DayPlan(
                            date = dayStart,
                            name = name,
                            status = DayStatus.PLANNED,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            version = 1,
                    )
                    dayPlanDao.insert(newPlan)
                    newPlan
                }
            }

        suspend fun updatePlanStatus(
            planId: String,
            status: DayStatus,
        ) = withContext(ioDispatcher) {
            dayPlanDao.updatePlanStatus(planId, status, System.currentTimeMillis())
        }

        suspend fun updatePredictedDurationMinutes(
            planId: String,
            minutes: Long?,
        ) = withContext(ioDispatcher) {
            val normalizedMinutes = minutes?.takeIf { it > 0L }
            dayPlanDao.updatePredictedDurationMinutes(
                planId = planId,
                minutes = normalizedMinutes,
                updatedAt = System.currentTimeMillis(),
            )
        }

        suspend fun updatePlanReflection(
            planId: String,
            reflection: String,
        ) = withContext(ioDispatcher) {
            val plan = dayPlanDao.getPlanById(planId) ?: return@withContext
            dayPlanDao.update(
                plan.copy(
                    reflection = reflection,
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = plan.version + 1,
                ),
            )
        }

        suspend fun updatePlanLinks(
            planId: String,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
        ) = withContext(ioDispatcher) {
            val plan = dayPlanDao.getPlanById(planId) ?: return@withContext
            dayPlanDao.update(
                plan.copy(
                    linkedProjectIds = linkedProjectIds.distinct(),
                    linkedAttachmentIds = linkedAttachmentIds.distinct(),
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = plan.version + 1,
                ),
            )
        }
        suspend fun addTaskToDayPlan(params: NewTaskParameters): DayTask =
            withContext(ioDispatcher) {
                val order =
                    params.order ?: run {
                        val maxOrder = dayTaskDao.getMaxOrderForDayPlan(params.dayPlanId) ?: 0L
                        maxOrder + 1
                    }
                val timing =
                    resolveTaskExecutionTiming(
                        scheduledTime = params.scheduledTime,
                        dueTime = params.dueTime,
                        duration = params.estimatedDurationMinutes,
                    )

                val task =
                    DayTask(
                        id = params.id ?: UUID.randomUUID().toString(),
                        dayPlanId = params.dayPlanId,
                        title = params.title,
                        description = params.description,
                        goalId = params.goalId,
                        projectId = params.projectId,
                        linkedProjectIds = params.linkedProjectIds ?: emptyList(),
                        linkedAttachmentIds = params.linkedAttachmentIds ?: emptyList(),
                        priority = params.priority,
                        scheduledTime = timing.scheduledTime,
                        estimatedDurationMinutes = params.estimatedDurationMinutes,
                        dueTime = timing.dueTime,
                        executionStrictness = params.executionStrictness,
                        order = order,
                        taskType = params.taskType ?: BacklogItemTypeValues.GOAL,
                        points = params.points,
                        syncedAt = null,
                        version = 1,
                )
                dayTaskDao.insert(task)
                taskExecutionAlarmCoordinator.sync(task)
                reorderTasksByPriority(params.dayPlanId)
                aiEventRepository.emit(
                    TaskCreatedEvent(
                        timestamp = java.time.Instant.ofEpochMilli(System.currentTimeMillis()),
                        effort = params.estimatedDurationMinutes?.toInt(),
                    ),
                )
                task
            }

        @Transaction
        suspend fun addGoalToDayPlan(
            dayPlanId: String,
            goalId: String,
            scheduledTime: Long? = null,
        ): DayTask =
            withContext(ioDispatcher) {
                val projectId =
                    listItemDao.findContextIdForGoal(goalId)
                        ?: throw IllegalStateException("Goal $goalId is not associated with any project.")

                val goal =
                    goalDao.getGoalById(goalId)
                        ?: throw NoSuchElementException("Goal with id $goalId not found")

                val taskParams =
                    NewTaskParameters(
                        dayPlanId = dayPlanId,
                        title = goal.text,
                        description = goal.description,
                        goalId = goalId,
                        projectId = projectId, // Крок 2.2: Передаємо знайдений projectId
                        linkedProjectIds = listOf(projectId),
                        scheduledTime = scheduledTime,
                        priority = mapImportanceToPriority(goal.valueImportance),
                        taskType = BacklogItemTypeValues.GOAL,
                    )
                addTaskToDayPlan(taskParams)
            }

        @Transaction
        suspend fun addProjectToDayPlan(
            dayPlanId: String,
            projectId: String,
            scheduledTime: Long? = null,
        ): DayTask =
            withContext(ioDispatcher) {
                val project =
                    contextDao.getContextById(projectId)
                        ?: throw NoSuchElementException("Project with id $projectId not found")

                val taskParams =
                    NewTaskParameters(
                        dayPlanId = dayPlanId,
                        title = project.name,
                        description = project.description,
                        projectId = projectId,
                        linkedProjectIds = listOf(projectId),
                        scheduledTime = scheduledTime,
                        priority = mapImportanceToPriority(project.valueImportance),
                        taskType = BacklogItemTypeValues.SUBLIST,
                    )
                addTaskToDayPlan(taskParams)
            }

        suspend fun copyTaskToTodaysPlan(taskToCopy: DayTask) =
            withContext(ioDispatcher) {
                val todayTimestamp = getDayStart(System.currentTimeMillis())
                val todaysPlan = createOrUpdateDayPlan(todayTimestamp)

                val newTaskParams =
                    NewTaskParameters(
                        dayPlanId = todaysPlan.id,
                        title = taskToCopy.title,
                        description = taskToCopy.description,
                        goalId = taskToCopy.goalId,
                        projectId = taskToCopy.projectId,
                        priority = taskToCopy.priority,
                        scheduledTime = null,
                        estimatedDurationMinutes = taskToCopy.estimatedDurationMinutes,
                        dueTime = taskToCopy.dueTime,
                        executionStrictness = taskToCopy.executionStrictness,
                        taskType = taskToCopy.taskType,
                        order = null,
                        points = taskToCopy.points,
                    )

                addTaskToDayPlan(newTaskParams)
            }

        suspend fun moveTaskToTomorrow(taskToMove: DayTask) =
            withContext(ioDispatcher) {
                val currentPlan = dayPlanDao.getPlanById(taskToMove.dayPlanId) ?: return@withContext

                val calendar =
                    Calendar.getInstance().apply {
                        timeInMillis = currentPlan.date
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                val tomorrowTimestamp = getDayStart(calendar.timeInMillis)
                val tomorrowsPlan = createOrUpdateDayPlan(tomorrowTimestamp)

                val maxOrder = dayTaskDao.getMaxOrderForDayPlan(tomorrowsPlan.id) ?: 0L

                val updatedTask =
                    taskToMove.copy(
                        dayPlanId = tomorrowsPlan.id,
                        order = maxOrder + 1, // Set order to be the last in the new plan
                        updatedAt = System.currentTimeMillis(),
                        syncedAt = null,
                        version = taskToMove.version + 1,
                    )
                dayTaskDao.update(updatedTask)
                reorderTasksByPriority(tomorrowsPlan.id)

                // Recalculate metrics for both days
                calculateAndSaveDailyMetrics(currentPlan.id)
                calculateAndSaveDailyMetrics(tomorrowsPlan.id)
                aiEventRepository.emit(
                    TaskDeferredEvent(
                        timestamp = java.time.Instant.ofEpochMilli(System.currentTimeMillis()),
                        taskId = taskToMove.id,
                    ),
                )
            }

        fun getTasksForDay(dayPlanId: String): Flow<List<DayTask>> =
            dayTaskDao.getTasksForDay(dayPlanId)
                .map { tasks ->

                    tasks.sortedWith(
                        compareBy<DayTask> { it.completed }
                            .thenBy { it.order }
                            .thenBy { it.title.lowercase() },
                    )
                }

        fun getTasksForGoal(goalId: String): Flow<List<DayTask>> = dayTaskDao.getTasksForGoal(goalId)

        suspend fun getTasksForDayOnce(dayPlanId: String): List<DayTask> =
            withContext(ioDispatcher) {
                dayTaskDao.getTasksForDaySync(dayPlanId).sortedWith(
                    compareBy<DayTask> { it.completed }
                        .thenBy { it.order }
                        .thenBy { it.title.lowercase() },
                )
            }

        @Transaction
        suspend fun updateTasksOrder(
            dayPlanId: String,
            reorderedTasks: List<DayTask>,
        ) = withContext(ioDispatcher) {
            reorderedTasks.forEach { task ->
                dayTaskDao.updateTaskOrder(
                    task.id,
                    task.order,
                    System.currentTimeMillis(),
                )
            }
            calculateAndSaveDailyMetrics(dayPlanId)
        }

        suspend fun updateTaskOrder(
            taskId: String,
            newOrder: Long,
        ) = withContext(ioDispatcher) {
            dayTaskDao.updateTaskOrder(taskId, newOrder, System.currentTimeMillis())
        }

        suspend fun completeTask(taskId: String) =
            withContext(ioDispatcher) {
                val now = System.currentTimeMillis()
                dayTaskDao.updateTaskCompletion(
                    taskId = taskId,
                    completed = true,
                    status = TaskStatus.COMPLETED,
                    completedAt = now,
                    updatedAt = now,
                )
                recalculateDayProgress(taskId)
                aiEventRepository.emit(
                    TaskCompletedEvent(
                        timestamp = java.time.Instant.ofEpochMilli(now),
                        xp = 0,
                        antiXp = 0,
                    ),
                )
            }

        suspend fun updateTask(params: UpdateTaskParams) = withContext(ioDispatcher) {
            val task = dayTaskDao.getTaskById(params.taskId) ?: return@withContext
            val timing =
                resolveTaskExecutionTiming(
                    scheduledTime = params.scheduledTime,
                    dueTime = params.dueTime,
                    duration = params.duration,
                )
            val updatedTask =
                task.copy(
                    title = params.title,
                    description = params.description,
                    priority = params.priority,
                    estimatedDurationMinutes = params.duration,
                    scheduledTime = timing.scheduledTime,
                    dueTime = timing.dueTime,
                    executionStrictness = params.executionStrictness,
                    points = params.points,
                    projectId = if (params.updateContextLinks) params.projectId else task.projectId,
                    linkedProjectIds =
                        if (params.updateContextLinks) {
                            params.linkedProjectIds.orEmpty()
                        } else {
                            task.linkedProjectIds
                        },
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = task.version + 1,
                )
            dayTaskDao.update(updatedTask)
            taskExecutionAlarmCoordinator.sync(updatedTask)
            reorderTasksByPriority(task.dayPlanId)
        }

        suspend fun startTaskWithTimeTracking(taskId: String): ActivityRecord? =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId) ?: return@withContext null
                val now = System.currentTimeMillis()

                val gId = task.goalId
                val pId = task.projectId

                val activityRecord =
                    when {
                        gId != null -> activityRepository.startGoalActivity(gId)
                        pId != null -> activityRepository.startContextActivity(pId)
                        else -> activityRepository.startActivity(task.title, now)
                    }

                activityRecord?.let {
                    val entityLinks =
                        buildList {
                            add(ActivityEntityLink(task.id, ActivityEntityType.DAY_TASK, task.dayPlanId))
                            task.projectId?.let { contextId ->
                                add(ActivityEntityLink(contextId, ActivityEntityType.CONTEXT))
                            }
                            task.goalId?.let { goalId ->
                                add(ActivityEntityLink(goalId, ActivityEntityType.GOAL))
                            }
                        }
                    activityRepository.updateRecord(
                        it.copy(
                            entityLinks = entityLinks,
                            contextId = task.projectId,
                            goalId = task.goalId,
                        ),
                    )
                    dayTaskDao.linkTaskWithActivity(taskId, it.id, now)
                    dayTaskDao.updateTaskCompletion(taskId, false, TaskStatus.IN_PROGRESS, null, now)
                    syncTaskTimingFromActualStart(taskId = taskId, actualStartTime = it.startTime ?: now)
                }
                activityRecord
            }

        suspend fun stopTaskTimeTracking(taskId: String) =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId) ?: return@withContext
                val now = System.currentTimeMillis()

                activityRepository.endLastActivity(now)

                task.activityRecordId?.let { recordId ->
                    val record = activityRepository.getActivityRecordById(recordId)
                    record?.durationInMillis?.let { duration ->
                        dayTaskDao.updateTaskDuration(taskId, duration / 60000, now)
                    }
                }
            }

        fun getMetricForDayStream(dayPlanId: String): Flow<DailyMetric?> =
            dailyMetricDao.getMetricForDayStream(
                dayPlanId,
            )

        fun getDailyAnalytics(
            startDate: Long,
            endDate: Long,
        ): Flow<List<DailyAnalytics>> {
            return combine(
                dayPlanDao.getPlansForDateRange(startDate, endDate),
                dailyMetricDao.getMetricsForDateRange(startDate, endDate),
            ) { plans, metrics ->
                plans.map { plan ->
                    val metric = metrics.find { it.dayPlanId == plan.id }
                    DailyAnalytics(
                        dayPlan = plan,
                        metric = metric,
                        completionRate = plan.completionPercentage,
                        totalTimeSpent = plan.totalCompletedMinutes,
                    )
                }
            }
        }

        suspend fun calculateAndSaveDailyMetrics(dayPlanId: String) =
            withContext(ioDispatcher) {
                cachedBestCompletedPoints = null

                val tasks = dayTaskDao.getTasksForDaySync(dayPlanId)
                val plan = dayPlanDao.getPlanById(dayPlanId) ?: return@withContext

                val completedTasks = tasks.count { it.completed }
                val totalTasks = tasks.size
                val completionRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

                val totalPlannedTime = tasks.mapNotNull { it.estimatedDurationMinutes }.sum()
                val totalActiveTime = tasks.mapNotNull { it.actualDurationMinutes }.sum()
                val completedPoints = tasks.filter { it.completed }.sumOf { it.points.coerceAtLeast(0) }

                val metric =
                    DailyMetric(
                        dayPlanId = dayPlanId,
                        date = plan.date,
                        tasksPlanned = totalTasks,
                        tasksCompleted = completedTasks,
                        completionRate = completionRate,
                        totalPlannedTime = totalPlannedTime,
                        totalActiveTime = totalActiveTime,
                        completedPoints = completedPoints,
                        updatedAt = System.currentTimeMillis(),
                        syncedAt = null,
                        version = 1,
                    )

                dailyMetricDao.insert(metric)

                dayPlanDao.updatePlanProgress(
                    planId = dayPlanId,
                    minutes = totalActiveTime,
                    percentage = completionRate,
                    updatedAt = System.currentTimeMillis(),
                )
            }

        fun getWeeklyInsights(startOfWeek: Long): Flow<WeeklyInsights> {
            val endOfWeek =
                startOfWeek +
                    (DAYS_IN_WEEK * HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLIS_IN_SECOND)
            return dailyMetricDao.getMetricsForDateRange(startOfWeek, endOfWeek).map { metrics ->
                if (metrics.isEmpty()) {
                    WeeklyInsights(
                        totalDays = 0,
                        averageCompletionRate = 0f,
                        totalActiveTime = 0,
                        averageTasksPerDay = 0f,
                        bestDay = null,
                        worstDay = null,
                        totalTasks = 0,
                        completedTasks = 0,
                    )
                } else {
                    val totalTasks = metrics.sumOf { it.tasksPlanned }
                    val completedTasks = metrics.sumOf { it.tasksCompleted }

                    WeeklyInsights(
                        totalDays = metrics.size,
                        averageCompletionRate = metrics.map { it.completionRate }.average().toFloat(),
                        totalActiveTime = metrics.sumOf { it.totalActiveTime },
                        averageTasksPerDay = metrics.map { it.tasksPlanned }.average().toFloat(),
                        bestDay = metrics.maxByOrNull { it.completionRate },
                        worstDay = metrics.minByOrNull { it.completionRate },
                        totalTasks = totalTasks,
                        completedTasks = completedTasks,
                    )
                }
            }
        }
        suspend fun materializeCanonicalRecurrenceForDate(date: Long) {
            val result = canonicalRecurrenceMaterializationAdapter.materializeForDate(date)

            android.util.Log.i(
                "ForwardSync",
                "canonical recurrence materialization " +
                    "date=$date status=${result.status} " +
                    "tasksCreated=${result.tasksToCreate.size} " +
                    "focusItemsCreated=${result.focusItemsToCreate.size} " +
                    "existingSkipped=${result.skippedExistingOccurrenceKeys.size}",
            )
        }

        private suspend fun resolveTaskExecutionTiming(
            scheduledTime: Long?,
            dueTime: Long?,
            duration: Long?,
            actualStartTime: Long? = null,
        ) = taskExecutionTimingCalculator.resolve(
            TaskExecutionTimingRequest(
                scheduledTime = scheduledTime,
                dueTime = dueTime,
                durationMinutes = duration,
                actualStartTime = actualStartTime,
            ),
        )

        private suspend fun syncTaskTimingFromActualStart(
            taskId: String,
            actualStartTime: Long,
        ) {
            val task = dayTaskDao.getTaskById(taskId) ?: return
            if (task.dueTime != null || task.estimatedDurationMinutes == null) return

            val timing =
                resolveTaskExecutionTiming(
                    scheduledTime = task.scheduledTime ?: actualStartTime,
                    dueTime = task.dueTime,
                    duration = task.estimatedDurationMinutes,
                    actualStartTime = actualStartTime,
                )
            val updatedTask =
                task.copy(
                    scheduledTime = task.scheduledTime ?: timing.scheduledTime,
                    dueTime = timing.dueTime,
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = task.version + 1,
                )
            dayTaskDao.update(updatedTask)
            taskExecutionAlarmCoordinator.sync(updatedTask)
        }

        private suspend fun reorderTasksByPriority(dayPlanId: String) {
            val tasks = dayTaskDao.getTasksForDaySync(dayPlanId)
            if (tasks.isEmpty()) return

            val reordered =
                tasks.sortedWith(
                    compareBy<DayTask> { it.completed }
                        .thenBy { taskPriorityRank(it.priority) }
                        .thenBy { it.dueTime ?: Long.MAX_VALUE }
                        .thenBy { it.order }
                        .thenBy { it.title.lowercase() },
                )

            reordered.forEachIndexed { index, task ->
                if (task.order != index.toLong()) {
                    dayTaskDao.updateTaskOrder(
                        taskId = task.id,
                        newOrder = index.toLong(),
                        updatedAt = System.currentTimeMillis(),
                    )
                }
            }
        }

        private fun taskPriorityRank(priority: TaskPriority): Int =
            when (priority) {
                TaskPriority.CRITICAL -> PRIORITY_RANK_CRITICAL
                TaskPriority.HIGH -> PRIORITY_RANK_HIGH
                TaskPriority.MEDIUM -> PRIORITY_RANK_MEDIUM
                TaskPriority.LOW -> PRIORITY_RANK_LOW
                TaskPriority.NONE -> PRIORITY_RANK_NONE
            }

        suspend fun getTaskById(taskId: String): DayTask? {
            return withContext(ioDispatcher) {
                dayTaskDao.getTaskById(taskId)
            }
        }

        suspend fun getGoal(id: String): Goal? {
            return withContext(ioDispatcher) {
                goalDao.getGoalById(id)
            }
        }

        suspend fun getProject(id: String): Context? {
            return withContext(ioDispatcher) {
                contextDao.getContextById(id)
            }
        }

        suspend fun getHighestCompletedPointsAcrossPlans(): Int =
            withContext(ioDispatcher) {
                cachedBestCompletedPoints
                    ?: dailyMetricDao.getMaxCompletedPoints()
                        .also { cachedBestCompletedPoints = it ?: 0 }
                    ?: 0
            }

        suspend fun findProjectIdForGoal(goalId: String): String? {
            return withContext(ioDispatcher) {
                listItemDao.findContextIdForGoal(goalId)
            }
        }

        private suspend fun recalculateDayProgress(taskId: String) {
            val task = dayTaskDao.getTaskById(taskId) ?: return
            calculateAndSaveDailyMetrics(task.dayPlanId)
        }

        private fun getDayStart(timestamp: Long): Long {
            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            return calendar.timeInMillis
        }

        private fun mapImportanceToPriority(importance: Float): TaskPriority {
            return when {
                importance >= 8f -> TaskPriority.CRITICAL
                importance >= 6f -> TaskPriority.HIGH
                importance >= 4f -> TaskPriority.MEDIUM
                else -> TaskPriority.LOW
            }
        }

        private fun priorityRank(priority: TaskPriority): Int =
            when (priority) {
                TaskPriority.CRITICAL -> 4
                TaskPriority.HIGH -> 3
                TaskPriority.MEDIUM -> 2
                TaskPriority.LOW -> 1
                TaskPriority.NONE -> 0
            }

        suspend fun deleteTask(taskId: String) =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId)
                if (task != null) {
                    taskExecutionAlarmCoordinator.cancel(task.id)
                    dayTaskDao.softDelete(
                        taskId = taskId,
                        updatedAt = System.currentTimeMillis(),
                    )
                    calculateAndSaveDailyMetrics(task.dayPlanId)
                }
            }

        suspend fun toggleTaskCompletion(taskId: String) =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId) ?: return@withContext
                val now = System.currentTimeMillis()
                val newStatus = !task.completed

                dayTaskDao.updateTaskCompletion(
                    taskId = taskId,
                    completed = newStatus,
                    status = if (newStatus) TaskStatus.COMPLETED else TaskStatus.NOT_STARTED,
                    completedAt = if (newStatus) now else null,
                    updatedAt = now,
                )

                if (newStatus) {
                    taskExecutionAlarmCoordinator.cancel(task.id)
                } else {
                    taskExecutionAlarmCoordinator.sync(task.copy(completed = false))
                }

                recalculateDayProgress(taskId)
            }
    }
