package com.romankozak.forwardappmobile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Transaction
    suspend fun insertGoalWithInstance(goal: Goal, instance: GoalInstance) {
        insertGoal(goal)
        insertInstance(instance)
    }

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): Goal?

    // ✨ ЗМІНА: Додаємо новий метод для отримання кількох цілей за їх ID
    @Query("SELECT * FROM goals WHERE id IN (:ids)")
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<Goal>

    @Transaction
    @Query("""
        SELECT goals.*, instances.instance_id, instances.listId AS list_id, instances.goal_order
        FROM goals
        INNER JOIN goal_instances AS instances ON goals.id = instances.goalId
        WHERE instances.listId = :listId
        ORDER BY instances.goal_order ASC
    """)
    fun getGoalsForListStream(listId: String): Flow<List<GoalWithInstanceInfo>>

    @Query("SELECT MIN(goal_order) FROM goal_instances WHERE listId = :listId")
    suspend fun getMinOrderForList(listId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: GoalInstance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalInstances(instances: List<GoalInstance>)

    @Query("DELETE FROM goal_instances WHERE instance_id = :instanceId")
    suspend fun deleteInstanceById(instanceId: String)

    @Query("DELETE FROM goal_instances WHERE listId IN (:listIds)")
    suspend fun deleteInstancesForLists(listIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreInstance(instance: GoalInstance)

    @Query("SELECT * FROM goal_instances")
    suspend fun getAllInstances(): List<GoalInstance>

    @Transaction
    suspend fun updateInstancesOrder(instances: List<GoalWithInstanceInfo>) {
        instances.forEach {
            updateInstance(it.toGoalInstance())
        }
    }

    @Update
    suspend fun updateInstance(instance: GoalInstance)

    @Query("UPDATE goal_instances SET listId = :targetListId WHERE instance_id = :instanceId")
    suspend fun updateInstanceListId(instanceId: String, targetListId: String)

    // ✨ ЗМІНА: Видаляємо старі, неправильні методи
    // fun getGoalIdListPairs(...) - ВИДАЛЕНО
    // fun getAssociatedListsForGoals(...) - ВИДАЛЕНО

    @Query("SELECT * FROM goals WHERE text LIKE '%' || :query || '%'")
    fun searchGoalsByText(query: String): Flow<List<Goal>>

    @Transaction
    @Query("""
        SELECT g.*, gl.id as listId, gl.name as listName
        FROM goals g
        JOIN goal_instances gi ON g.id = gi.goalId
        JOIN goal_lists gl ON gi.listId = gl.id
        WHERE g.text LIKE :query
    """)
    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult>

    @Query("SELECT count(*) FROM goal_instances WHERE listId = :listId")
    suspend fun getGoalCountInList(listId: String): Int
}