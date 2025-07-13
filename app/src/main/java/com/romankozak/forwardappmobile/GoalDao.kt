package com.romankozak.forwardappmobile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map // Імпортуємо map

@Dao
interface GoalDao {

    // Внутрішня функція без логування, яку викликає обгорнута функція
    @Query("""
        SELECT goals.*, goal_instances.id AS instanceId, goal_instances.orderIndex
        FROM goals
        INNER JOIN goal_instances ON goals.id = goal_instances.goalId
        WHERE goal_instances.listId = :listId
        ORDER BY goal_instances.orderIndex ASC
    """)
    fun getGoalsForListInternal(listId: String): Flow<List<GoalWithInstanceInfo>>

    // Обгорнута функція з логуванням
    fun getGoalsForList(listId: String): Flow<List<GoalWithInstanceInfo>> {
        return getGoalsForListInternal(listId).map { goals ->
            println("DEBUG_DAO: getGoalsForList emitted ${goals.size} goals for list $listId. First goal: ${goals.firstOrNull()?.goal?.text}")
            goals
        }
    }

    @Query("SELECT * FROM goal_lists WHERE id = :listId")
    fun getList(listId: String): Flow<GoalList?>

    // --- НОВІ МЕТОДИ ДЛЯ ПЕРЕМІЩЕННЯ ЦІЛЕЙ ---
    @Query("SELECT * FROM goal_lists")
    fun getAllLists(): Flow<List<GoalList>>

    // Внутрішня функція без логування
    @Query("UPDATE goal_instances SET listId = :newListId, orderIndex = :newOrderIndex WHERE id = :instanceId")
    suspend fun moveGoalToNewListInternal(instanceId: String, newListId: String, newOrderIndex: Int)

    // Обгорнута функція з логуванням
    suspend fun moveGoalToNewList(instanceId: String, newListId: String, newOrderIndex: Int) {
        println("DEBUG_DAO: moveGoalToNewList: Instance $instanceId to list $newListId at index $newOrderIndex")
        moveGoalToNewListInternal(instanceId, newListId, newOrderIndex)
    }
    // -----------------------------------------

    // --- ДОДАНО: Метод для глобального пошуку цілей ---
    @Query("""
        SELECT g.*, gl.id as listId, gl.name as listName
        FROM goals g
        INNER JOIN goal_instances gi ON g.id = gi.goalId
        INNER JOIN goal_lists gl ON gi.listId = gl.id
        WHERE g.text LIKE :query
        GROUP BY g.id
        ORDER BY g.createdAt DESC
    """)
    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult>


    // --- Функції для синхронізації ---
    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<Goal>

    @Query("SELECT * FROM goal_instances")
    suspend fun getAllInstances(): List<GoalInstance>

    // Внутрішня функція без логування
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalsInternal(goals: List<Goal>)

    // Обгорнута функція з логуванням
    suspend fun insertGoals(goals: List<Goal>) {
        println("DEBUG_DAO: Insert/Replace ${goals.size} Goals (batch)")
        insertGoalsInternal(goals)
    }

    // Внутрішня функція без логування
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalInstancesInternal(instances: List<GoalInstance>)

    // Обгорнута функція з логуванням
    suspend fun insertGoalInstances(instances: List<GoalInstance>) {
        println("DEBUG_DAO: Insert/Replace ${instances.size} GoalInstances (batch)")
        insertGoalInstancesInternal(instances)
    }

    @Query("DELETE FROM goal_instances WHERE listId IN (:listIds)")
    suspend fun deleteInstancesForLists(listIds: List<String>) {
        println("DEBUG_DAO: Delete Instances for Lists: $listIds")
        deleteInstancesForListsInternal(listIds)
    }

    @Query("DELETE FROM goal_instances WHERE listId IN (:listIds)")
    suspend fun deleteInstancesForListsInternal(listIds: List<String>)

    // --- Керування окремими цілями ---
    // Внутрішня функція без логування
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalInternal(goal: Goal)

    // Обгорнута функція з логуванням
    suspend fun insertGoal(goal: Goal) {
        println("DEBUG_DAO: Insert/Replace Goal: ${goal.text} (ID: ${goal.id})")
        insertGoalInternal(goal)
    }

    // Внутрішня функція без логування
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalInstanceInternal(instance: GoalInstance)

    // Обгорнута функція з логуванням
    suspend fun insertGoalInstance(instance: GoalInstance) {
        println("DEBUG_DAO: Insert/Replace GoalInstance: (ID: ${instance.id}, GoalID: ${instance.goalId}, ListID: ${instance.listId})")
        insertGoalInstanceInternal(instance)
    }

    // Внутрішня функція без логування
    @Update
    suspend fun updateGoalInternal(goal: Goal)

    // Обгорнута функція з логуванням
    suspend fun updateGoal(goal: Goal) {
        println("DEBUG_DAO: Update Goal: ${goal.text} (ID: ${goal.id})")
        updateGoalInternal(goal)
    }

    // Внутрішня функція без логування
    @Update
    suspend fun updateGoalInstancesInternal(instances: List<GoalInstance>)

    // Обгорнута функція з логуванням
    suspend fun updateGoalInstances(instances: List<GoalInstance>) {
        println("DEBUG_DAO: Update GoalInstances: ${instances.size} instances.")
        updateGoalInstancesInternal(instances)
    }

    // Внутрішня функція без логування
    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalByIdInternal(goalId: String)

    // Обгорнута функція з логуванням
    suspend fun deleteGoalById(goalId: String) {
        println("DEBUG_DAO: Delete Goal by ID: $goalId")
        deleteGoalByIdInternal(goalId)
    }

    // Внутрішня функція без логування
    @Query("DELETE FROM goal_instances WHERE goalId = :goalId")
    suspend fun deleteGoalInstancesByGoalIdInternal(goalId: String)

    // Обгорнута функція з логуванням
    suspend fun deleteGoalInstancesByGoalId(goalId: String) {
        println("DEBUG_DAO: Delete GoalInstances by Goal ID: $goalId")
        deleteGoalInstancesByGoalIdInternal(goalId)
    }

    // ДОДАНО: Новий метод для точкового видалення екземпляра
    @Query("DELETE FROM goal_instances WHERE id = :instanceId")
    suspend fun deleteGoalInstanceById(instanceId: String)

    @Query("SELECT COUNT(*) FROM goal_instances WHERE listId = :listId")
    suspend fun getGoalCountInList(listId: String): Int

}