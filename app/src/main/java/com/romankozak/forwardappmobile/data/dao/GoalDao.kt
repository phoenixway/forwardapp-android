package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult
import com.romankozak.forwardappmobile.data.database.models.Goal
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

    @Query("SELECT * FROM goals WHERE text LIKE '%' || :query || '%'")
    fun searchGoalsByText(query: String): Flow<List<Goal>>

    @Transaction
    @Query(
        """
WITH RECURSIVE path_cte(id, name, path) AS (
    -- Базовий випадок: вибираємо кореневі списки (у яких немає батьків)
    SELECT id, name, name as path
    FROM goal_lists
    WHERE parentId IS NULL

    UNION ALL

    -- Рекурсивний крок: приєднуємо дочірні списки та доповнюємо шлях
    SELECT gl.id, gl.name, p.path || ' / ' || gl.name
    FROM goal_lists gl
    JOIN path_cte p ON gl.parentId = p.id
)
SELECT DISTINCT g.*, gl.id as listId, gl.name as listName, pc.path as pathSegments
FROM goals g
JOIN list_items li ON g.id = li.entityId AND li.itemType = 'GOAL'
JOIN goal_lists gl ON li.listId = gl.id
JOIN path_cte pc ON gl.id = pc.id -- Приєднуємо згенерований шлях до кожного списку
WHERE g.text LIKE :query
""",
    )
    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult>

    @Query("SELECT COUNT(*) FROM goals")
    fun getAllGoalsCountFlow(): Flow<Int>

    @Query("UPDATE goals SET description = :markdown WHERE id = :goalId")
    suspend fun updateMarkdown(
        goalId: String,
        markdown: String,
    )

    @Query("DELETE FROM goal_lists WHERE id = :listId")
    suspend fun deleteGoalListById(listId: String)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
