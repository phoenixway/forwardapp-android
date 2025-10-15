
package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
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
        private val listItemDao: ListItemDao, 
        private val activityRepository: ActivityRepository,
        private val alarmScheduler: javax.inject.Provider<AlarmScheduler>,
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
            dayPlanId: String,
            goalId: String? = null
        ) {
            withContext(ioDispatcher) {
                val dayPlan = dayPlanDao.getPlanById(dayPlanId) ?: return@withContext
                val recurringTask = RecurringTask(
                    title = title,
                    description = description,
                    goalId = goalId,
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
                    goalId = goalId,
                    estimatedDurationMinutes = duration,
                    taskType = ListItemTypeValues.GOAL,
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
                    taskType = params.taskType ?: ListItemTypeValues.GOAL,
                    points = params.points,
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

            val projectId = listItemDao.findProjectIdForGoal(goalId)
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

                    scheduledTime = scheduledTime,
                    priority = mapImportanceToPriority(goal.valueImportance),
                    taskType = ListItemTypeValues.GOAL,
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
                        taskType = ListItemTypeValues.SUBLIST,
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

        suspend fun moveTaskToTomorrow(taskToMove: DayTask) = withContext(ioDispatcher) {
            val currentPlan = dayPlanDao.getPlanById(taskToMove.dayPlanId) ?: return@withContext

            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentPlan.date
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowTimestamp = getDayStart(calendar.timeInMillis)
            val tomorrowsPlan = createOrUpdateDayPlan(tomorrowTimestamp)

            val maxOrder = dayTaskDao.getMaxOrderForDayPlan(tomorrowsPlan.id) ?: 0L

            val updatedTask = taskToMove.copy(
                dayPlanId = tomorrowsPlan.id,
                order = maxOrder + 1, // Set order to be the last in the new plan
                updatedAt = System.currentTimeMillis()
            )
            dayTaskDao.update(updatedTask)

            // Recalculate metrics for both days
            calculateAndSaveDailyMetrics(currentPlan.id)
            calculateAndSaveDailyMetrics(tomorrowsPlan.id)
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
            points: Int,
        ) = withContext(ioDispatcher) {
            val task = dayTaskDao.getTaskById(taskId) ?: return@withContext
            val updatedTask =
                task.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    estimatedDurationMinutes = duration,
                    points = points,
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
            newDuration: Long?,
            points: Int
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
                    endDate = null,
                    points = points
                )
                recurringTaskDao.insert(newRecurringTask)

                // 3. Delete future instances of the old task
                val futureDayPlanIds = dayPlanDao.getFutureDayPlanIds(dayPlan.date)
                if (futureDayPlanIds.isNotEmpty()) {
                    dayTaskDao.deleteTasksForDayPlanIds(recurringTaskId, futureDayPlanIds)
                }
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

                                val title: String
                                val description: String?
                                val goalId: String?

                                if (recurringTask.goalId != null) {
                                    val goal = goalDao.getGoalById(recurringTask.goalId)
                                    if (goal != null) {
                                        title = goal.text
                                        description = goal.description
                                        goalId = goal.id
                                    } else {
                                        // Goal was deleted, skip this occurrence
                                        return@forEach
                                    }
                                } else {
                                    title = recurringTask.title
                                    description = recurringTask.description
                                    goalId = null
                                }

                                val taskParams = NewTaskParameters(
                                    dayPlanId = dayPlan.id,
                                    title = title,
                                    description = description,
                                    goalId = goalId,
                                    priority = recurringTask.priority,
                                    estimatedDurationMinutes = recurringTask.duration?.toLong(),
                                    taskType = ListItemTypeValues.GOAL,
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
                RecurrenceFrequency.HOURLY -> true
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
                RecurrenceFrequency.MONTHLY -> {
                    val startCalendar = Calendar.getInstance().apply { timeInMillis = recurringTask.startDate }
                    calendar.get(Calendar.DAY_OF_MONTH) == startCalendar.get(Calendar.DAY_OF_MONTH)
                }
                RecurrenceFrequency.YEARLY -> {
                    val startCalendar = Calendar.getInstance().apply { timeInMillis = recurringTask.startDate }
                    calendar.get(Calendar.DAY_OF_YEAR) == startCalendar.get(Calendar.DAY_OF_YEAR)
                }
            }
        }

        suspend fun deleteAllFutureInstancesOfRecurringTask(recurringTaskId: String, dayPlanId: String) {
            withContext(ioDispatcher) {
                val TAG = "DELETE_RECURRING_DEBUG"
                android.util.Log.d(TAG, "Deleting future instances for recurringTaskId: $recurringTaskId, dayPlanId: $dayPlanId")

                val recurringTask = recurringTaskDao.getById(recurringTaskId)
                if (recurringTask != null) {
                    android.util.Log.d(TAG, "Found recurringTask: $recurringTask")
                    val dayPlan = dayPlanDao.getPlanById(dayPlanId)
                    if (dayPlan != null) {
                        android.util.Log.d(TAG, "Found dayPlan: $dayPlan")
                        val yesterday = Calendar.getInstance().apply {
                            timeInMillis = dayPlan.date
                            add(Calendar.DAY_OF_YEAR, -1)
                        }.timeInMillis
                        android.util.Log.d(TAG, "Setting endDate to: $yesterday")
                        recurringTaskDao.update(recurringTask.copy(endDate = yesterday))

                        val futureDayPlanIds = dayPlanDao.getFutureDayPlanIds(dayPlan.date)
                        android.util.Log.d(TAG, "Found future day plan IDs: $futureDayPlanIds")
                        if (futureDayPlanIds.isNotEmpty()) {
                            android.util.Log.d(TAG, "Deleting tasks for future day plans")
                            dayTaskDao.deleteTasksForDayPlanIds(recurringTaskId, futureDayPlanIds)
                        } else {
                            android.util.Log.d(TAG, "No future day plans found to delete tasks from.")
                        }
                    } else {
                        android.util.Log.d(TAG, "Could not find dayPlan with id: $dayPlanId")
                    }
                } else {
                    android.util.Log.d(TAG, "Could not find recurringTask with id: $recurringTaskId")
                }
            }
        }

            suspend fun getRecurringTask(id: String): RecurringTask? {
                return withContext(ioDispatcher) {
                    recurringTaskDao.getById(id)
                }
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

                recalculateDayProgress(taskId)
            }

    }
