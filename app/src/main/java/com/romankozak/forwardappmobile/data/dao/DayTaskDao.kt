
package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.database.models.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DayTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DayTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<DayTask>)

    @Update
    suspend fun update(task: DayTask)

    @Update
    suspend fun updateAll(tasks: List<DayTask>)

    @Delete
    suspend fun delete(task: DayTask)

    @Query("DELETE FROM day_tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM day_tasks WHERE id IN (:taskIds)")
    suspend fun deleteByIds(taskIds: List<String>)

    @Query("SELECT * FROM day_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): DayTask?

    @Query("SELECT * FROM day_tasks WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getTasksForGoal(goalId: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksForProject(projectId: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND status = :status ORDER BY `order` ASC")
    fun getTasksByStatus(
        dayPlanId: String,
        status: TaskStatus,
    ): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND priority = :priority ORDER BY `order` ASC")
    fun getTasksByPriority(
        dayPlanId: String,
        priority: TaskPriority,
    ): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND completed = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(dayPlanId: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND completed = 0 ORDER BY `order` ASC, priority DESC")
    fun getPendingTasks(dayPlanId: String): Flow<List<DayTask>>

    @Query(
        "SELECT * FROM day_tasks WHERE scheduledTime IS NOT NULL AND scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime ASC",
    )
    fun getScheduledTasksInRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<DayTask>>

    @Query("DELETE FROM day_tasks WHERE dayPlanId = :dayPlanId")
    suspend fun clearTasksForDay(dayPlanId: String)

    @Query("SELECT COUNT(*) FROM day_tasks WHERE dayPlanId = :dayPlanId")
    suspend fun getTaskCountForDay(dayPlanId: String): Int

    @Query("SELECT COUNT(*) FROM day_tasks WHERE dayPlanId = :dayPlanId AND completed = 1")
    suspend fun getCompletedTaskCountForDay(dayPlanId: String): Int

    
    @Query(
        """
        SELECT * FROM day_tasks 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%' 
        OR notes LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """,
    )
    suspend fun searchTasks(query: String): List<DayTask>

    @Query("SELECT MAX(`order`) FROM day_tasks WHERE dayPlanId = :dayPlanId")
    suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long?

    @Query("UPDATE day_tasks SET `order` = :newOrder, updatedAt = :updatedAt, version = version + 1, syncedAt = NULL WHERE id = :taskId")
    suspend fun updateTaskOrder(
        taskId: String,
        newOrder: Long,
        updatedAt: Long,
    )

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId ORDER BY completed ASC, `order` ASC, title ASC")
    suspend fun getTasksForDaySync(dayPlanId: String): List<DayTask>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId ORDER BY completed ASC, `order` ASC, title ASC")
    fun getTasksForDay(dayPlanId: String): Flow<List<DayTask>>

    @Query("UPDATE day_tasks SET activityRecordId = :activityRecordId, updatedAt = :updatedAt, version = version + 1, syncedAt = NULL WHERE id = :taskId")
    suspend fun linkTaskWithActivity(
        taskId: String,
        activityRecordId: String,
        updatedAt: Long,
    )

    @Query("UPDATE day_tasks SET actualDurationMinutes = :durationMinutes, updatedAt = :updatedAt, version = version + 1, syncedAt = NULL WHERE id = :taskId")
    suspend fun updateTaskDuration(
        taskId: String,
        durationMinutes: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE day_tasks SET 
        completed = :completed, 
        status = :status, 
        completedAt = :completedAt, 
        updatedAt = :updatedAt,
        version = version + 1,
        syncedAt = NULL
        WHERE id = :taskId
    """,
    )
    suspend fun updateTaskCompletion(
        taskId: String,
        completed: Boolean,
        status: TaskStatus,
        completedAt: Long?,
        updatedAt: Long,
    )



    @Query("SELECT * FROM day_tasks WHERE recurringTaskId = :recurringTaskId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findTemplateForRecurringTask(recurringTaskId: String): DayTask?

    @Query("SELECT * FROM day_tasks WHERE recurringTaskId = :recurringTaskId AND dayPlanId = :dayPlanId LIMIT 1")
    suspend fun findByRecurringIdAndDate(recurringTaskId: String, dayPlanId: String): DayTask?

    @Query("DELETE FROM day_tasks WHERE recurringTaskId = :recurringTaskId AND dayPlanId IN (:dayPlanIds)")
    suspend fun deleteTasksForDayPlanIds(recurringTaskId: String, dayPlanIds: List<String>)

    @Query("UPDATE day_tasks SET recurringTaskId = null WHERE id = :taskId")
    suspend fun detachFromRecurrence(taskId: String)

    @Query("UPDATE day_tasks SET nextOccurrenceTime = :nextOccurrenceTime WHERE id = :taskId")
    suspend fun updateNextOccurrenceTime(taskId: String, nextOccurrenceTime: Long)

}
