
package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.Flow

@Dao
interface DayTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaw(task: DayTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRaw(tasks: List<DayTask>)

    @Update
    suspend fun updateRaw(task: DayTask)

    @Update
    suspend fun updateAllRaw(tasks: List<DayTask>)

    @Query("SELECT * FROM workspaces WHERE id = :workspaceId LIMIT 1")
    suspend fun getOperationalProjectWorkspace(workspaceId: String): WorkspaceEntity?

    @Transaction
    suspend fun insert(task: DayTask) {
        insertRaw(
            routeDayTaskProjectForPersistence(task) { workspaceId ->
                getOperationalProjectWorkspace(workspaceId)
            },
        )
    }

    @Transaction
    suspend fun insertAll(tasks: List<DayTask>) {
        if (tasks.isEmpty()) return
        insertAllRaw(
            tasks.map { task ->
                routeDayTaskProjectForPersistence(task) { workspaceId ->
                    getOperationalProjectWorkspace(workspaceId)
                }
            },
        )
    }

    @Transaction
    suspend fun update(task: DayTask) {
        updateRaw(
            routeDayTaskProjectForPersistence(task) { workspaceId ->
                getOperationalProjectWorkspace(workspaceId)
            },
        )
    }

    @Transaction
    suspend fun updateAll(tasks: List<DayTask>) {
        if (tasks.isEmpty()) return
        updateAllRaw(
            tasks.map { task ->
                routeDayTaskProjectForPersistence(task) { workspaceId ->
                    getOperationalProjectWorkspace(workspaceId)
                }
            },
        )
    }

    @Query("SELECT * FROM day_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getByIdForCanonicalRecurrenceSync(taskId: String): DayTask?

    @Delete
    suspend fun delete(task: DayTask)

    @Query("DELETE FROM day_tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query(
        """
        UPDATE day_tasks
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :taskId
          AND isDeleted = 0
        """,
    )
    suspend fun softDelete(taskId: String, updatedAt: Long)

    @Query("DELETE FROM day_tasks WHERE id IN (:taskIds)")
    suspend fun deleteByIds(taskIds: List<String>)

    @Query("SELECT * FROM day_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): DayTask?

    @Query("SELECT * FROM day_tasks WHERE isDeleted = 0")
    fun getAllVisibleTasksFlow(): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getTasksForGoal(goalId: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE (projectId = :projectId OR project_workspace_id = :projectId) ORDER BY createdAt DESC")
    fun getTasksForProject(projectId: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND status = :status AND isDeleted = 0 ORDER BY `order` ASC")
    fun getTasksByStatus(
        dayPlanId: String,
        status: TaskStatus,
    ): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND priority = :priority AND isDeleted = 0 ORDER BY `order` ASC")
    fun getTasksByPriority(
        dayPlanId: String,
        priority: TaskPriority,
    ): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND completed = 1 AND isDeleted = 0 ORDER BY completedAt DESC")
    fun getCompletedTasks(dayPlanId: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND completed = 0 AND isDeleted = 0 ORDER BY `order` ASC, priority DESC")
    fun getPendingTasks(dayPlanId: String): Flow<List<DayTask>>

    @Query(
        """
        SELECT * FROM day_tasks
        WHERE scheduledTime IS NOT NULL
          AND isDeleted = 0
          AND scheduledTime BETWEEN :startTime AND :endTime
        ORDER BY scheduledTime ASC
        """,
    )
    fun getScheduledTasksInRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<DayTask>>

    @Query("DELETE FROM day_tasks WHERE dayPlanId = :dayPlanId")
    suspend fun clearTasksForDay(dayPlanId: String)

    @Query("SELECT COUNT(*) FROM day_tasks WHERE dayPlanId = :dayPlanId AND isDeleted = 0")
    suspend fun getTaskCountForDay(dayPlanId: String): Int

    @Query("SELECT COUNT(*) FROM day_tasks WHERE dayPlanId = :dayPlanId AND completed = 1 AND isDeleted = 0")
    suspend fun getCompletedTaskCountForDay(dayPlanId: String): Int

    @Query(
        """
        SELECT * FROM day_tasks 
        WHERE isDeleted = 0
        AND (
            title LIKE '%' || :query || '%'
            OR description LIKE '%' || :query || '%'
            OR notes LIKE '%' || :query || '%'
        )
        ORDER BY createdAt DESC
    """,
    )
    suspend fun searchTasks(query: String): List<DayTask>

    @Query("SELECT MAX(`order`) FROM day_tasks WHERE dayPlanId = :dayPlanId AND isDeleted = 0")
    suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long?

    @Query("SELECT MIN(`order`) FROM day_tasks WHERE dayPlanId = :dayPlanId AND isDeleted = 0")
    suspend fun getMinOrderForDayPlan(dayPlanId: String): Long?

    @Query(
        """
        UPDATE day_tasks
        SET `order` = :newOrder,
            updatedAt = :updatedAt,
            version = version + 1,
            syncedAt = NULL
        WHERE id = :taskId
        """,
    )
    suspend fun updateTaskOrder(
        taskId: String,
        newOrder: Long,
        updatedAt: Long,
    )

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND isDeleted = 0 ORDER BY completed ASC, `order` ASC, title ASC")
    suspend fun getTasksForDaySync(dayPlanId: String): List<DayTask>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId ORDER BY completed ASC, `order` ASC, title ASC")
    suspend fun getTasksForDayIncludingDeletedSync(dayPlanId: String): List<DayTask>

    @Query("SELECT * FROM day_tasks WHERE dayPlanId = :dayPlanId AND isDeleted = 0 ORDER BY completed ASC, `order` ASC, title ASC")
    fun getTasksForDay(dayPlanId: String): Flow<List<DayTask>>

    @Query(
        """
        UPDATE day_tasks
        SET activityRecordId = :activityRecordId,
            updatedAt = :updatedAt,
            version = version + 1,
            syncedAt = NULL
        WHERE id = :taskId
        """,
    )
    suspend fun linkTaskWithActivity(
        taskId: String,
        activityRecordId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE day_tasks
        SET actualDurationMinutes = :durationMinutes,
            updatedAt = :updatedAt,
            version = version + 1,
            syncedAt = NULL
        WHERE id = :taskId
        """,
    )
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

    // --- Backup Methods ---
    @Query("SELECT * FROM day_tasks")
    suspend fun getAllTasksSync(): List<DayTask>

    @Query("DELETE FROM day_tasks")
    suspend fun deleteAllTasks()

    @Transaction
    suspend fun insertTasks(tasks: List<DayTask>) {
        if (tasks.isEmpty()) return
        insertAllRaw(
            tasks.map { task ->
                routeDayTaskProjectForPersistence(task) { workspaceId ->
                    getOperationalProjectWorkspace(workspaceId)
                }
            },
        )
    }
}

internal suspend fun routeDayTaskProjectForPersistence(
    task: DayTask,
    workspaceLookup: suspend (String) -> WorkspaceEntity?,
): DayTask {
    val contextProjectId = task.projectId
    val workspaceProjectId = task.projectWorkspaceId

    require(
        contextProjectId == null ||
            workspaceProjectId == null ||
            contextProjectId == workspaceProjectId,
    ) {
        "DayTask ${task.id} has conflicting project ids: " +
            "context=$contextProjectId workspace=$workspaceProjectId"
    }

    val logicalProjectId =
        task.logicalProjectId
            ?: return task.copy(
                projectId = null,
                projectWorkspaceId = null,
            )

    return if (SystemContexts.isSystem(ContextId(logicalProjectId))) {
        val workspace =
            requireNotNull(workspaceLookup(logicalProjectId)) {
                "Reserved DayTask project $logicalProjectId has no same-id Workspace"
            }

        require(
            !workspace.isDeleted &&
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                workspace.sourceContextId == null,
        ) {
            "Reserved DayTask project $logicalProjectId " +
                "is not a live CANONICAL_ONLY Workspace"
        }

        task.copy(
            projectId = null,
            projectWorkspaceId = logicalProjectId,
        )
    } else {
        task.copy(
            projectId = logicalProjectId,
            projectWorkspaceId = null,
        )
    }
}
