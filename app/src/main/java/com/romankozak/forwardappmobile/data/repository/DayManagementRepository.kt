package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторій для управління всіма аспектами щоденного планування,
 * завдань, метрик та аналітики.
 */
@Singleton
class DayManagementRepository @Inject constructor( // <-- @Inject constructor - це ключова інструкція для Hilt
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    private val activityRepository: ActivityRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // === Day Plan Operations ===

    /** Отримує план за ID для реактивного оновлення в UI. */
    fun getPlanByIdStream(planId: String): Flow<DayPlan?> =
        dayPlanDao.getPlanByIdStream(planId).flowOn(ioDispatcher)

    /** Отримує план на конкретну дату. */
    fun getPlanForDate(date: Long): Flow<DayPlan?> =
        dayPlanDao.getPlanForDate(getDayStart(date)).flowOn(ioDispatcher)

    /** Створює новий план, якщо не існує, або оновлює існуючий. */
    suspend fun createOrUpdateDayPlan(date: Long, name: String? = null): DayPlan = withContext(ioDispatcher) {
        val dayStart = getDayStart(date)
        val existingPlan = dayPlanDao.getPlanForDateSync(dayStart)

        if (existingPlan != null) {
            val updated = existingPlan.copy(
                name = name ?: existingPlan.name,
                updatedAt = System.currentTimeMillis()
            )
            dayPlanDao.update(updated)
            updated
        } else {
            val newPlan = DayPlan(
                date = dayStart,
                name = name,
                status = DayStatus.PLANNED
            )
            dayPlanDao.insert(newPlan)
            newPlan
        }
    }

    /** Оновлює статус плану дня. */
    suspend fun updatePlanStatus(planId: String, status: DayStatus) = withContext(ioDispatcher) {
        dayPlanDao.updatePlanStatus(planId, status, System.currentTimeMillis())
    }

    /** Оновлює рефлексію для плану дня. */
    suspend fun updatePlanReflection(planId: String, reflection: String) = withContext(ioDispatcher) {
        val plan = dayPlanDao.getPlanById(planId) ?: return@withContext
        dayPlanDao.update(
            plan.copy(
                reflection = reflection,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // === Task Operations ===

    /**
     * Додає нове завдання до плану дня, використовуючи об'єкт параметрів.
     * Це вирішує попередження про велику кількість параметрів.
     */
    suspend fun addTaskToDayPlan(params: NewTaskParameters): DayTask = withContext(ioDispatcher) {
        val task = DayTask(
            dayPlanId = params.dayPlanId,
            title = params.title,
            description = params.description,
            goalId = params.goalId,
            projectId = params.projectId,
            priority = params.priority,
            scheduledTime = params.scheduledTime,
            estimatedDurationMinutes = params.estimatedDurationMinutes,
            order = System.currentTimeMillis() // Використовуємо час для початкового сортування
        )
        dayTaskDao.insert(task)
        task
    }

    /** Додає існуючу ціль як завдання на день. Викидає виняток, якщо ціль не знайдена. */
    @Transaction
    suspend fun addGoalToDayPlan(dayPlanId: String, goalId: String, scheduledTime: Long? = null): DayTask = withContext(ioDispatcher) {
        val goal = goalDao.getGoalById(goalId)
            ?: throw NoSuchElementException("Goal with id $goalId not found")

        val taskParams = NewTaskParameters(
            dayPlanId = dayPlanId,
            title = goal.text,
            description = goal.description,
            goalId = goalId,
            scheduledTime = scheduledTime,
            priority = mapImportanceToPriority(goal.valueImportance)
        )
        addTaskToDayPlan(taskParams)
    }

    /** Додає існуючий проєкт як завдання на день. Викидає виняток, якщо проєкт не знайдений. */
    @Transaction
    suspend fun addProjectToDayPlan(dayPlanId: String, projectId: String, scheduledTime: Long? = null): DayTask = withContext(ioDispatcher) {
        val project = goalListDao.getGoalListById(projectId)
            ?: throw NoSuchElementException("Project with id $projectId not found")

        val taskParams = NewTaskParameters(
            dayPlanId = dayPlanId,
            title = project.name,
            description = project.description,
            projectId = projectId,
            scheduledTime = scheduledTime,
            priority = mapImportanceToPriority(project.valueImportance)
        )
        addTaskToDayPlan(taskParams)
    }

    fun getTasksForDay(dayPlanId: String): Flow<List<DayTask>> =
        dayTaskDao.getTasksForDay(dayPlanId).flowOn(ioDispatcher)

    fun getTasksForGoal(goalId: String): Flow<List<DayTask>> =
        dayTaskDao.getTasksForGoal(goalId).flowOn(ioDispatcher)

    /** Позначає завдання як виконане та перераховує прогрес дня. */
    suspend fun completeTask(taskId: String) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        dayTaskDao.updateTaskCompletion(
            taskId = taskId,
            completed = true,
            status = TaskStatus.COMPLETED,
            completedAt = now,
            updatedAt = now
        )
        recalculateDayProgress(taskId)
    }

    /** Запускає відстеження часу для завдання, створюючи відповідний ActivityRecord. */
    suspend fun startTaskWithTimeTracking(taskId: String): ActivityRecord? = withContext(ioDispatcher) {
        val task = dayTaskDao.getTaskById(taskId) ?: return@withContext null
        val now = System.currentTimeMillis()

        val activityRecord = when {
            task.goalId != null -> activityRepository.startGoalActivity(task.goalId)
            task.projectId != null -> activityRepository.startListActivity(task.projectId)
            else -> activityRepository.startActivity(task.title, now)
        }

        activityRecord?.let {
            dayTaskDao.linkTaskWithActivity(taskId, it.id, now)
            dayTaskDao.updateTaskCompletion(taskId, false, TaskStatus.IN_PROGRESS, null, now)
        }
        activityRecord
    }

    /** Зупиняє відстеження часу для завдання. */
    suspend fun stopTaskTimeTracking(taskId: String) = withContext(ioDispatcher) {
        val task = dayTaskDao.getTaskById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()

        activityRepository.endLastActivity(now)

        // Оновлюємо тривалість задачі, отримуючи дані через ActivityRepository
        task.activityRecordId?.let { recordId ->
            // Примітка: в ActivityRepository має бути метод для отримання запису за ID
            val record = activityRepository.getActivityRecordById(recordId)
            record?.durationInMillis?.let { duration ->
                dayTaskDao.updateTaskDuration(taskId, duration / 60000, now) // конвертуємо в хвилини
            }
        }
    }

    // === Analytics and Insights ===

    /** Отримує метрики за ID для реактивного оновлення в UI. */
    fun getMetricForDayStream(dayPlanId: String): Flow<DailyMetric?> =
        dailyMetricDao.getMetricForDayStream(dayPlanId).flowOn(ioDispatcher)

    fun getDailyAnalytics(startDate: Long, endDate: Long): Flow<List<DailyAnalytics>> {
        return combine(
            dayPlanDao.getPlansForDateRange(startDate, endDate),
            dailyMetricDao.getMetricsForDateRange(startDate, endDate)
        ) { plans, metrics ->
            plans.map { plan ->
                val metric = metrics.find { it.dayPlanId == plan.id }
                DailyAnalytics(
                    dayPlan = plan,
                    metric = metric,
                    completionRate = plan.completionPercentage,
                    totalTimeSpent = plan.totalCompletedMinutes
                )
            }
        }.flowOn(ioDispatcher)
    }

    /** Обчислює та зберігає зведені метрики за день. */
    suspend fun calculateAndSaveDailyMetrics(dayPlanId: String) = withContext(ioDispatcher) {
        val tasks = dayTaskDao.getTasksForDaySync(dayPlanId)
        val plan = dayPlanDao.getPlanById(dayPlanId) ?: return@withContext

        val completedTasks = tasks.count { it.completed }
        val totalTasks = tasks.size
        val completionRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

        val totalPlannedTime = tasks.mapNotNull { it.estimatedDurationMinutes }.sum()
        val totalActiveTime = tasks.mapNotNull { it.actualDurationMinutes }.sum()

        val metric = DailyMetric(
            dayPlanId = dayPlanId,
            date = plan.date,
            tasksPlanned = totalTasks,
            tasksCompleted = completedTasks,
            completionRate = completionRate,
            totalPlannedTime = totalPlannedTime,
            totalActiveTime = totalActiveTime
        )

        dailyMetricDao.insert(metric)

        dayPlanDao.updatePlanProgress(
            planId = dayPlanId,
            minutes = totalActiveTime,
            percentage = completionRate,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun getWeeklyInsights(startOfWeek: Long): Flow<WeeklyInsights> {
        val endOfWeek = startOfWeek + (7 * 24 * 60 * 60 * 1000L) // Додано дужки для ясності
        return dailyMetricDao.getMetricsForDateRange(startOfWeek, endOfWeek).map { metrics ->
            WeeklyInsights(
                totalDays = metrics.size,
                averageCompletionRate = metrics.map { it.completionRate }.average().toFloat(),
                totalActiveTime = metrics.sumOf { it.totalActiveTime },
                averageTasksPerDay = metrics.map { it.tasksPlanned }.average().toFloat(),
                bestDay = metrics.maxByOrNull { it.completionRate },
                worstDay = metrics.minByOrNull { it.completionRate }
            )
        }.flowOn(ioDispatcher)
    }

    // === Helper Functions ===

    private suspend fun recalculateDayProgress(taskId: String) {
        val task = dayTaskDao.getTaskById(taskId) ?: return
        calculateAndSaveDailyMetrics(task.dayPlanId)
    }

    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /** Єдина функція для мапінгу важливості на пріоритет. */
    private fun mapImportanceToPriority(importance: Float): TaskPriority {
        return when {
            importance >= 8f -> TaskPriority.CRITICAL
            importance >= 6f -> TaskPriority.HIGH
            importance >= 4f -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }
    }
    /** Перемикає статус виконання завдання. */
    suspend fun toggleTaskCompletion(taskId: String) = withContext(ioDispatcher) {
        val task = dayTaskDao.getTaskById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()
        val newStatus = !task.completed

        dayTaskDao.updateTaskCompletion(
            taskId = taskId,
            completed = newStatus,
            status = if (newStatus) TaskStatus.COMPLETED else TaskStatus.NOT_STARTED,
            completedAt = if (newStatus) now else null,
            updatedAt = now
        )
        recalculateDayProgress(taskId)
    }

}