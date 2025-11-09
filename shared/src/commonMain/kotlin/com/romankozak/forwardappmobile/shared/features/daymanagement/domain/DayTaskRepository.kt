package com.romankozak.forwardappmobile.shared.features.daymanagement.domain

import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface DayTaskRepository {
    fun getDayTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>>
    fun getDayTaskById(id: String): Flow<DayTask?>
    suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long
    fun getTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>>
    fun getTasksForGoal(goalId: String): Flow<List<DayTask>>
    suspend fun getTasksForDayPlanOnce(dayPlanId: String): List<DayTask>
    suspend fun updateTaskOrder(taskId: String, newOrder: Long, updatedAt: Long)
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean, status: TaskStatus, completedAt: Long?, updatedAt: Long)
    suspend fun deleteTasksForDayPlanIds(recurringTaskId: String, dayPlanIds: List<String>)
    suspend fun linkTaskWithActivity(taskId: String, activityRecordId: String, updatedAt: Long)
    suspend fun updateTaskDuration(taskId: String, durationMinutes: Long, updatedAt: Long)
    suspend fun findByRecurringIdAndDayPlanId(recurringTaskId: String, dayPlanId: String): DayTask?
    suspend fun findTemplateForRecurringTask(recurringTaskId: String): DayTask?
    suspend fun detachFromRecurrence(taskId: String)
    suspend fun updateNextOccurrenceTime(taskId: String, nextOccurrenceTime: Long)
    suspend fun insertDayTask(dayTask: DayTask)
    suspend fun updateDayTask(dayTask: DayTask)
    suspend fun deleteDayTask(id: String)
    suspend fun deleteAllDayTasksForDayPlan(dayPlanId: String)
}
