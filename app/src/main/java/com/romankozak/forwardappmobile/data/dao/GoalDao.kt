package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import com.romankozak.forwardappmobile.data.database.models.toGoalInstance
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    // ✨ ЗМІНА: Додано метод для оновлення кількох цілей одночасно
    @Update
    suspend fun updateGoals(goals: List<Goal>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Transaction
    suspend fun insertGoalWithInstance(goal: Goal, instance: GoalInstance) {
        insertGoal(goal)
        insertInstance(instance)
    }

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): Goal?

    @Query("SELECT * FROM goals WHERE id IN (:ids)")
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>

    // ✨ ЗМІНА: Додано suspend-версію для одноразового отримання кількох цілей
    @Query("SELECT * FROM goals WHERE id IN (:ids)")
    suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal>

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

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertInstance(instance: GoalInstance)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertGoalInstances(instances: List<GoalInstance>)

    @Query("DELETE FROM goal_instances WHERE instance_id = :instanceId")
    suspend fun deleteInstanceById(instanceId: String)

    // ✨ ЗМІНА: Додано метод для видалення кількох екземплярів за їх ID
    @Query("DELETE FROM goal_instances WHERE instance_id IN (:instanceIds)")
    suspend fun deleteInstancesByIds(instanceIds: List<String>)

    @Query("DELETE FROM goal_instances WHERE listId IN (:listIds)")
    suspend fun deleteInstancesForLists(listIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
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

    // ✨ ЗМІНА: Додано метод для переміщення кількох екземплярів до іншого списку
    @Query("UPDATE goal_instances SET listId = :targetListId WHERE instance_id IN (:instanceIds)")
    suspend fun updateInstanceListIds(instanceIds: List<String>, targetListId: String)

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


    @Query("SELECT COUNT(*) FROM goals")
    fun getAllGoalsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM goal_instances WHERE goalId = :goalId AND listId = :listId")
    suspend fun getInstanceCount(goalId: String, listId: String): Int

    @Query("DELETE FROM goal_instances WHERE goalId = :goalId AND listId = :listId")
    suspend fun deleteInstanceByGoalAndList(goalId: String, listId: String)
}