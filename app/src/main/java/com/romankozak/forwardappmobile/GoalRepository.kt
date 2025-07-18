package com.romankozak.forwardappmobile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao
) {
    // Методи для GoalDetailViewModel
    fun getGoalsForListStream(listId: String): Flow<List<GoalWithInstanceInfo>> {
        return goalDao.getGoalsForListStream(listId)
    }

    suspend fun getGoalListById(id: String): GoalList? {
        return goalListDao.getGoalListById(id)
    }

    fun getAssociatedListsForGoal(goalId: String): Flow<Map<String, List<GoalList>>> {
        return goalDao.getAssociatedListsForGoals(listOf(goalId))
    }

    suspend fun createGoal(title: String, listId: String) {
        val currentTime = System.currentTimeMillis()
        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            text = title,
            description = "",
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime,
            tags = null,
            associatedListIds = listOf(listId)
        )

        val order = goalDao.getGoalCountInList(listId).toLong()
        val newInstance = GoalInstance(
            instanceId = UUID.randomUUID().toString(),
            goalId = newGoal.id,
            listId = listId,
            order = order
        )

        goalDao.insertGoalWithInstance(newGoal, newInstance)
    }

    suspend fun deleteGoalInstance(instanceId: String) {
        goalDao.deleteInstanceById(instanceId)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun createGoalInstance(goalId: String, targetListId: String) {
        val order = goalDao.getGoalCountInList(targetListId).toLong()
        val newInstance = GoalInstance(
            instanceId = UUID.randomUUID().toString(),
            goalId = goalId,
            listId = targetListId,
            order = order
        )
        goalDao.insertInstance(newInstance)
    }

    suspend fun moveGoalInstance(instanceId: String, targetListId: String) {
        goalDao.updateInstanceListId(instanceId, targetListId)
    }

    suspend fun copyGoal(goal: Goal, targetListId: String) {
        val newGoal = goal.copy(id = UUID.randomUUID().toString())
        createGoalInstance(newGoal.id, targetListId)
        goalDao.insertGoal(newGoal)
    }

    // Методи для GoalListViewModel
    fun getAllGoalListsFlow(): Flow<List<GoalList>> {
        return goalListDao.getAllLists()
    }

    suspend fun updateGoalList(list: GoalList) {
        goalListDao.update(list)
    }

    suspend fun createGoalList(name: String, parentId: String?) {
        val newList = GoalList(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId,
            description = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        goalListDao.insert(newList)
    }

    suspend fun deleteListsAndSubLists(listsToDelete: List<GoalList>) {
        val listIds = listsToDelete.map { it.id }
        goalDao.deleteInstancesForLists(listIds)
        listsToDelete.forEach { goalListDao.delete(it) }
    }

    // --- ДОДАНО МЕТОДИ ДЛЯ SyncRepository ---
    suspend fun getAllGoalLists(): List<GoalList> {
        return goalListDao.getAll()
    }

    suspend fun getAllGoals(): List<Goal> {
        return goalDao.getAll()
    }

    suspend fun getAllGoalInstances(): List<GoalInstance> {
        return goalDao.getAllInstances()
    }

    suspend fun insertGoalLists(lists: List<GoalList>) {
        goalListDao.insertLists(lists)
    }

    suspend fun insertGoals(goals: List<Goal>) {
        goalDao.insertGoals(goals)
    }

    suspend fun deleteInstancesForLists(listIds: List<String>) {
        goalDao.deleteInstancesForLists(listIds)
    }

    suspend fun insertGoalInstances(instances: List<GoalInstance>) {
        goalDao.insertGoalInstances(instances)
    }

    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult> {
        return goalDao.searchGoalsGlobal(query)
    }
// ... всередині класу GoalRepository

    // --- ДОДАНО МЕТОДИ ДЛЯ GoalEditViewModel ---
    suspend fun getGoalById(id: String): Goal? {
        return goalDao.getGoalById(id)
    }

    suspend fun getListsByIds(ids: List<String>): List<GoalList> {
        return goalListDao.getListsByIds(ids)
    }

    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    suspend fun getGoalCountInList(listId: String): Int {
        return goalDao.getGoalCountInList(listId)
    }

    suspend fun insertInstance(instance: GoalInstance) {
        goalDao.insertInstance(instance)
    }

    fun getAssociatedListsForGoals(goalIds: List<String>): Flow<Map<String, List<GoalList>>> {
        if (goalIds.isEmpty()) return flowOf(emptyMap())
        return goalDao.getAssociatedListsForGoals(goalIds)
    }

    fun getGoalListByIdFlow(id: String): Flow<GoalList?> {
        return goalListDao.getGoalListByIdStream(id)
    }



}