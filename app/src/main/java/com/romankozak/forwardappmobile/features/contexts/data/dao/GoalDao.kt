package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalGoalSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Update
    suspend fun updateGoals(goals: List<Goal>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): Goal?

    @Query("SELECT * FROM goals WHERE id IN (:ids)")
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id IN (:ids)")
    suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<Goal>

    @Query("SELECT * FROM goals WHERE is_deleted = 0")
    suspend fun getAllVisible(): List<Goal>

    @Query("SELECT * FROM goals")
    fun getAllGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE is_deleted = 0")
    fun getAllVisibleGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE text LIKE '%' || :query || '%' AND is_deleted = 0")
    fun searchGoalsByText(query: String): Flow<List<Goal>>

    @Transaction
    @Query(
        """
        WITH RECURSIVE path_cte(id, name, path) AS (
            SELECT id, name, name as path FROM contexts WHERE parentId IS NULL
            UNION ALL
            SELECT p.id, p.name, pct.path || ' / ' || p.name
            FROM contexts p JOIN path_cte pct ON p.parentId = pct.id
        ),
        goal_contexts(goalId, contextId) AS (
            SELECT mapping.sourceId, entry.workspaceId
            FROM workspace_backlog_entries entry
            JOIN legacy_subject_mappings mapping
              ON mapping.subjectId = entry.targetId
            WHERE entry.targetKind = 'ORIENTATION'
              AND entry.isDeleted = 0
              AND mapping.sourceType = 'GOAL'
              AND mapping.state = 'CUT_OVER'
              AND mapping.isDeleted = 0

            UNION

            SELECT bg.goal_id, bg.context_id
            FROM backlog_goal_association_links bg
        )
        SELECT DISTINCT
            g.*,
            p.id as contextId,
            p.name as contextName,
            pc.path as pathSegments
        FROM goals g
        JOIN goal_contexts gc ON gc.goalId = g.id
        JOIN contexts p ON gc.contextId = p.id
        JOIN path_cte pc ON p.id = pc.id
        WHERE (g.text LIKE :query OR g.description LIKE :query)
          AND g.is_deleted = 0
        """,
    )
    suspend fun searchGoalsGlobal(query: String): List<GlobalGoalSearchResult>

    @Query("SELECT COUNT(*) FROM goals WHERE is_deleted = 0")
    fun getAllGoalsCountFlow(): Flow<Int>

    @Query("UPDATE goals SET description = :markdown WHERE id = :goalId")
    suspend fun updateMarkdown(
        goalId: String,
        markdown: String,
    )

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    @Query("SELECT * FROM goals")
    suspend fun getAllRaw(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<Goal>)

    @Query(
        """
        SELECT DISTINCT g.*
        FROM goals g
        WHERE g.is_deleted = 0
          AND (
            EXISTS (
                SELECT 1
                FROM workspace_backlog_entries entry
                JOIN legacy_subject_mappings mapping
                  ON mapping.subjectId = entry.targetId
                WHERE entry.workspaceId = :contextId
                  AND entry.targetKind = 'ORIENTATION'
                  AND entry.isDeleted = 0
                  AND mapping.sourceType = 'GOAL'
                  AND mapping.sourceId = g.id
                  AND mapping.state = 'CUT_OVER'
                  AND mapping.isDeleted = 0
            )
            OR EXISTS (
                SELECT 1
                FROM backlog_goal_association_links bg
                WHERE bg.goal_id = g.id
                  AND bg.context_id = :contextId
            )
          )
        """,
    )
    fun getGoalsByContextIdFlow(contextId: String): Flow<List<Goal>>
}
