
package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayManagementRepository
    @Inject
    constructor(
        private val dayPlanDao: DayPlanDao,
        private val dayTaskDao: DayTaskDao,
        private val dailyMetricDao: DailyMetricDao,
        private val goalDao: GoalDao,
        private val projectDao: ProjectDao,
        private val recurringTaskDao: RecurringTaskDao,
        private val activityRepository: ActivityRepository,
        private val alarmScheduler: AlarmScheduler,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        

        fun getPlanByIdStream(planId: String): Flow<DayPlan?> = dayPlanDao.getPlanByIdStream(planId)

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
                    val updated =
                        existingPlan.copy(
                            name = name ?: existingPlan.name,
                            updatedAt = System.currentTimeMillis(),
                        )
                    dayPlanDao.update(updated)
                    updated
                } else {
                    val newPlan =
                        DayPlan(
                            date = dayStart,
                            name = name,
                            status = DayStatus.PLANNED,
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

        suspend fun updatePlanReflection(
            planId: String,
            reflection: String,
        ) = withContext(ioDispatcher) {
            val plan = dayPlanDao.getPlanById(planId) ?: return@withContext
            dayPlanDao.update(
                plan.copy(
                    reflection = reflection,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        suspend fun addRecurringTask(
            title: String,
            description: String?,
            duration: Long?,
            priority: TaskPriority,
            recurrenceRule: RecurrenceRule,
            dayPlanId: String
        ) {
            withContext(ioDispatcher) {
                val dayPlan = dayPlanDao.getPlanById(dayPlanId) ?: return@withContext
                val recurringTask = RecurringTask(
                    title = title,
                    description = description,
                    duration = duration?.toInt(),
                    priority = priority,
                    recurrenceRule = recurrenceRule,
                    startDate = dayPlan.date
                )
                recurringTaskDao.insert(recurringTask)

                val taskParams = NewTaskParameters(
                    dayPlanId = dayPlanId,
                    title = title,
                    description = description,
                    priority = priority,
                    estimatedDurationMinutes = duration,
                    taskType = ListItemType.GOAL,
                )
                val dayTask = addTaskToDayPlan(taskParams).copy(
                    recurringTaskId = recurringTask.id,
                    nextOccurrenceTime = if (recurrenceRule.frequency == RecurrenceFrequency.HOURLY) System.currentTimeMillis() else null
                )
                dayTaskDao.update(dayTask)
            }
        }

        

        suspend fun addTaskToDayPlan(params: NewTaskParameters): DayTask =
            withContext(ioDispatcher) {
                val order =
                    params.order ?: run {
                        val maxOrder = dayTaskDao.getMaxOrderForDayPlan(params.dayPlanId) ?: 0L
                        maxOrder + 1
                    }

                val task =
                    DayTask(
                        dayPlanId = params.dayPlanId,
                        title = params.title,
                        description = params.description,
                        goalId = params.goalId,
                        projectId = params.projectId,
                        priority = params.priority,
                        scheduledTime = params.scheduledTime,
                        estimatedDurationMinutes = params.estimatedDurationMinutes,
                        order = order,
                        
                        taskType = params.taskType ?: ListItemType.GOAL,
                    )
                dayTaskDao.insert(task)
                task
            }

        @Transaction
        suspend fun addGoalToDayPlan(
            dayPlanId: String,
            goalId: String,
            scheduledTime: Long? = null,
        ): DayTask =
            withContext(ioDispatcher) {
                val goal =
                    goalDao.getGoalById(goalId)
                        ?: throw NoSuchElementException("Goal with id $goalId not found")

                val taskParams =
                    NewTaskParameters(
                        dayPlanId = dayPlanId,
                        title = goal.text,
                        description = goal.description,
                        goalId = goalId,
                        scheduledTime = scheduledTime,
                        priority = mapImportanceToPriority(goal.valueImportance),
                        taskType = ListItemType.GOAL,
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
                    projectDao.getProjectById(projectId)
                        ?: throw NoSuchElementException("Project with id $projectId not found")

                val taskParams =
                    NewTaskParameters(
                        dayPlanId = dayPlanId,
                        title = project.name,
                        description = project.description,
                        projectId = projectId,
                        scheduledTime = scheduledTime,
                        priority = mapImportanceToPriority(project.valueImportance),
                        taskType = ListItemType.SUBLIST,
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
                        taskType = taskToCopy.taskType,
                        order = null,
                    )

                
                addTaskToDayPlan(newTaskParams)
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
                dayTaskDao.updateTaskOrder(task.id, task.order, System.currentTimeMillis())
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
            }

        suspend fun updateTask(
            taskId: String,
            title: String,
            description: String?,
            priority: TaskPriority,
            duration: Long?,
        ) = withContext(ioDispatcher) {
            val task = dayTaskDao.getTaskById(taskId) ?: return@withContext
            val updatedTask =
                task.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    estimatedDurationMinutes = duration,
                    updatedAt = System.currentTimeMillis(),
                )
            dayTaskDao.update(updatedTask)
        }

        suspend fun updateRecurringTaskTemplate(
            recurringTaskId: String,
            title: String,
            description: String?,
            priority: TaskPriority,
            duration: Long?,
        ) = withContext(ioDispatcher) {
            val task = recurringTaskDao.getById(recurringTaskId) ?: return@withContext
            val updatedTask =
                task.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    duration = duration?.toInt(),
                )
            recurringTaskDao.update(updatedTask)
        }

        @Transaction
        suspend fun splitRecurringTask(
            originalTask: DayTask,
            newTitle: String,
            newDescription: String?,
            newPriority: TaskPriority,
            newDuration: Long?
        ) {
            withContext(ioDispatcher) {
                val recurringTaskId = originalTask.recurringTaskId ?: return@withContext
                val originalRecurringTask = recurringTaskDao.getById(recurringTaskId) ?: return@withContext
                val dayPlan = dayPlanDao.getPlanById(originalTask.dayPlanId) ?: return@withContext

                // 1. End the old recurring task
                val yesterday = Calendar.getInstance().apply {
                    timeInMillis = dayPlan.date
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
                recurringTaskDao.update(originalRecurringTask.copy(endDate = yesterday))

                // 2. Create a new recurring task
                val newRecurringTask = originalRecurringTask.copy(
                    id = UUID.randomUUID().toString(),
                    title = newTitle,
                    description = newDescription,
                    priority = newPriority,
                    duration = newDuration?.toInt(),
                    startDate = dayPlan.date,
                    endDate = null
                )
                recurringTaskDao.insert(newRecurringTask)

                // 3. Delete future instances of the old task
                dayTaskDao.deleteFutureInstances(recurringTaskId, dayPlan.date)
            }
        }

        suspend fun startTaskWithTimeTracking(taskId: String): ActivityRecord? =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId) ?: return@withContext null
                val now = System.currentTimeMillis()

                val activityRecord =
                    when {
                        task.goalId != null -> activityRepository.startGoalActivity(task.goalId)
                        task.projectId != null -> activityRepository.startProjectActivity(task.projectId)
                        else -> activityRepository.startActivity(task.title, now)
                    }

                activityRecord?.let {
                    dayTaskDao.linkTaskWithActivity(taskId, it.id, now)
                    dayTaskDao.updateTaskCompletion(taskId, false, TaskStatus.IN_PROGRESS, null, now)
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
                val tasks = dayTaskDao.getTasksForDaySync(dayPlanId)
                val plan = dayPlanDao.getPlanById(dayPlanId) ?: return@withContext

                val completedTasks = tasks.count { it.completed }
                val totalTasks = tasks.size
                val completionRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

                val totalPlannedTime = tasks.mapNotNull { it.estimatedDurationMinutes }.sum()
                val totalActiveTime = tasks.mapNotNull { it.actualDurationMinutes }.sum()

                val metric =
                    DailyMetric(
                        dayPlanId = dayPlanId,
                        date = plan.date,
                        tasksPlanned = totalTasks,
                        tasksCompleted = completedTasks,
                        completionRate = completionRate,
                        totalPlannedTime = totalPlannedTime,
                        totalActiveTime = totalActiveTime,
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
            val endOfWeek = startOfWeek + (7 * 24 * 60 * 60 * 1000L)
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

        suspend fun generateRecurringTasksForDate(date: Long) {
            withContext(ioDispatcher) {
                val dayPlan = dayPlanDao.getPlanForDateSync(date)
                if (dayPlan != null) {
                    val recurringTasks = recurringTaskDao.getAll()
                    recurringTasks.forEach { recurringTask ->
                        if (shouldGenerateTaskForDate(recurringTask, date)) {
                            val existingTask = dayTaskDao.findByRecurringIdAndDate(recurringTask.id, dayPlan.id)
                            if (existingTask == null) {
                                val taskParams = NewTaskParameters(
                                    dayPlanId = dayPlan.id,
                                    title = recurringTask.title,
                                    description = recurringTask.description,
                                    priority = recurringTask.priority,
                                    estimatedDurationMinutes = recurringTask.duration?.toLong(),
                                    taskType = ListItemType.GOAL,
                                )
                                addTaskToDayPlan(taskParams).copy(recurringTaskId = recurringTask.id).also { dayTaskDao.update(it) }
                            }
                        }
                    }
                }
            }
        }

        private fun shouldGenerateTaskForDate(recurringTask: RecurringTask, date: Long): Boolean {
            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (date < recurringTask.startDate) return false
            if (recurringTask.endDate != null && date > recurringTask.endDate) return false

            return when (recurringTask.recurrenceRule.frequency) {
                RecurrenceFrequency.DAILY -> true
                RecurrenceFrequency.WEEKLY -> {
                    val taskDayOfWeek = when(dayOfWeek) {
                        Calendar.MONDAY -> java.time.DayOfWeek.MONDAY
                        Calendar.TUESDAY -> java.time.DayOfWeek.TUESDAY
                        Calendar.WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
                        Calendar.THURSDAY -> java.time.DayOfWeek.THURSDAY
                        Calendar.FRIDAY -> java.time.DayOfWeek.FRIDAY
                        Calendar.SATURDAY -> java.time.DayOfWeek.SATURDAY
                        Calendar.SUNDAY -> java.time.DayOfWeek.SUNDAY
                        else -> null
                    }
                    recurringTask.recurrenceRule.daysOfWeek?.contains(taskDayOfWeek) ?: false
                }
                // TODO: Implement MONTHLY and YEARLY
                else -> false
            }
        }

        suspend fun deleteAllFutureInstancesOfRecurringTask(recurringTaskId: String, dayPlanId: String) {
            withContext(ioDispatcher) {
                val recurringTask = recurringTaskDao.getById(recurringTaskId)
                if (recurringTask != null) {
                    val dayPlan = dayPlanDao.getPlanById(dayPlanId)
                    if (dayPlan != null) {
                        val yesterday = Calendar.getInstance().apply {
                            timeInMillis = dayPlan.date
                            add(Calendar.DAY_OF_YEAR, -1)
                        }.timeInMillis
                        recurringTaskDao.update(recurringTask.copy(endDate = yesterday))
                        dayTaskDao.deleteFutureInstances(recurringTaskId, dayPlan.date)
                    }
                }
            }
        }

        suspend fun getRecurringTask(id: String): RecurringTask? {
            return withContext(ioDispatcher) {
                recurringTaskDao.getById(id)
            }
        }

        suspend fun detachFromRecurrence(taskId: String) {
            withContext(ioDispatcher) {
                dayTaskDao.detachFromRecurrence(taskId)
            }
        }

        suspend fun updateTaskNextOccurrence(taskId: String, nextOccurrenceTime: Long) {
            withContext(ioDispatcher) {
                dayTaskDao.updateNextOccurrenceTime(taskId, nextOccurrenceTime)
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

        
        suspend fun deleteTask(taskId: String) =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId)
                if (task != null) {
                    
                    if (task.reminderTime != null) {
                        alarmScheduler.cancelForTask(task)
                    }
                    dayTaskDao.deleteById(taskId)
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

                
                if (task.reminderTime != null) {
                    if (newStatus) {
                        alarmScheduler.cancelForTask(task)
                    } else {
                        alarmScheduler.scheduleForTask(task)
                    }
                }

                recalculateDayProgress(taskId)
            }

        suspend fun setTaskReminder(
            taskId: String,
            reminderTime: Long,
        ) = withContext(ioDispatcher) {
            dayTaskDao.updateReminderTime(taskId, reminderTime, System.currentTimeMillis())
            val updatedTask = dayTaskDao.getTaskById(taskId)
            if (updatedTask != null) {
                alarmScheduler.scheduleForTask(updatedTask)
            }
        }

        suspend fun clearTaskReminder(taskId: String) =
            withContext(ioDispatcher) {
                val task = dayTaskDao.getTaskById(taskId)
                dayTaskDao.updateReminderTime(taskId, null, System.currentTimeMillis())
                if (task != null) {
                    alarmScheduler.cancelForTask(task)
                }
            }
    }
