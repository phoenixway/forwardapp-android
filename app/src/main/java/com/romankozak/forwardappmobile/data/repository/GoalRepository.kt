// Файл: app/src/main/java/com/romankozak/forwardappmobile/data/repository/GoalRepository.kt

package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.dao.RecentListDao
import com.romankozak.forwardappmobile.data.database.models.RecentListEntry
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// ✨ ВИПРАВЛЕНО: Змінено 'private' на 'internal' для вирішення помилки компіляції
internal enum class ContextTextAction { ADD, REMOVE }

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    private val recentListDao: RecentListDao,
    private val contextHandlerProvider: Provider<ContextHandler>
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }

    suspend fun createGoalWithInstance(title: String, listId: String): String {
        val currentTime = System.currentTimeMillis()
        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            text = title,
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        goalDao.insertGoal(newGoal)

        syncContextMarker(newGoal.id, listId, ContextTextAction.ADD)

        val newInstance = GoalInstance(
            instanceId = UUID.randomUUID().toString(),
            goalId = newGoal.id,
            listId = listId,
            order = -currentTime
        )
        goalDao.insertInstance(newInstance)

        val finalGoalState = goalDao.getGoalById(newGoal.id)!!
        contextHandler.handleContextsOnCreate(finalGoalState)

        return newInstance.instanceId
    }

    suspend fun deleteGoalInstances(instanceIds: List<String>) {
        if (instanceIds.isNotEmpty()) {
            val instances = goalDao.getInstancesByIds(instanceIds)
            instances.forEach { instance ->
                syncContextMarker(instance.goalId, instance.listId, ContextTextAction.REMOVE)
            }
            goalDao.deleteInstancesByIds(instanceIds)
        }
    }

    suspend fun moveGoalInstances(instanceIds: List<String>, targetListId: String) {
        if (instanceIds.isNotEmpty()) {
            val instances = goalDao.getInstancesByIds(instanceIds)
            instances.forEach { instance ->
                syncContextMarker(instance.goalId, instance.listId, ContextTextAction.REMOVE)
                syncContextMarker(instance.goalId, targetListId, ContextTextAction.ADD)
            }
            goalDao.updateInstanceListIds(instanceIds, targetListId)
        }
    }

    suspend fun createGoalInstances(goalIds: List<String>, targetListId: String) {
        if (goalIds.isNotEmpty()) {
            goalIds.forEach { goalId ->
                syncContextMarker(goalId, targetListId, ContextTextAction.ADD)
            }
            val newInstances = goalIds.map { goalId ->
                GoalInstance(
                    instanceId = UUID.randomUUID().toString(),
                    goalId = goalId,
                    listId = targetListId,
                    order = -System.currentTimeMillis()
                )
            }
            goalDao.insertGoalInstances(newInstances)
        }
    }

    suspend fun copyGoals(goalIds: List<String>, targetListId: String, markerToAdd: String?) {
        if (goalIds.isNotEmpty()) {
            val originalGoals = goalDao.getGoalsByIdsSuspend(goalIds)
            val newGoals = mutableListOf<Goal>()
            val newInstances = mutableListOf<GoalInstance>()

            originalGoals.forEach { goal ->
                var newGoalText = goal.text
                if (markerToAdd != null && !goal.text.contains(markerToAdd)) {
                    newGoalText = "${goal.text} $markerToAdd".trim()
                }
                val newGoal = goal.copy(
                    id = UUID.randomUUID().toString(),
                    text = newGoalText
                )
                newGoals.add(newGoal)
                newInstances.add(
                    GoalInstance(
                        instanceId = UUID.randomUUID().toString(),
                        goalId = newGoal.id,
                        listId = targetListId,
                        order = -System.currentTimeMillis()
                    )
                )
            }
            goalDao.insertGoals(newGoals)
            goalDao.insertGoalInstances(newInstances)
        }
    }

    private suspend fun syncContextMarker(goalId: String, listId: String, action: ContextTextAction) {
        val list = goalListDao.getGoalListById(listId) ?: return
        val listTags = list.tags.orEmpty()
        if (listTags.isEmpty()) return

        val tagMap = contextHandler.tagToContextNameMap.value
        val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in listTags }?.value ?: return
        val marker = contextHandler.getContextMarker(contextName) ?: return
        val goal = goalDao.getGoalById(goalId) ?: return

        val hasMarker = goal.text.contains(marker)
        var newText = goal.text

        if (action == ContextTextAction.ADD && !hasMarker) {
            newText = "${goal.text} $marker".trim()
        } else if (action == ContextTextAction.REMOVE && hasMarker) {
            newText = goal.text.replace(Regex("\\s*${Regex.escape(marker)}\\s*"), " ").trim()
        }

        if (newText != goal.text) {
            goalDao.updateGoal(goal.copy(text = newText, updatedAt = System.currentTimeMillis()))
        }
    }

    // --- Публічні методи, необхідні для GoalListViewModel ---

    suspend fun updateGoalList(list: GoalList) {
        goalListDao.update(list)
    }

    suspend fun updateGoalLists(lists: List<GoalList>) {
        lists.forEach { goalListDao.update(it) }
    }

    @Transaction
    suspend fun deleteListsAndSubLists(listsToDelete: List<GoalList>) {
        if (listsToDelete.isEmpty()) return
        val listIds = listsToDelete.map { it.id }
        goalDao.deleteInstancesForLists(listIds)
        listsToDelete.forEach { goalListDao.delete(it) }
    }

    @Transaction
    suspend fun moveGoalList(listToMove: GoalList, newParentId: String?) {
        val listFromDb = goalListDao.getGoalListById(listToMove.id) ?: return
        val oldParentId = listFromDb.parentId

        if (oldParentId != newParentId) {
            val oldSiblings = (if (oldParentId != null) {
                goalListDao.getListsByParentId(oldParentId)
            } else {
                goalListDao.getTopLevelLists()
            }).filter { it.id != listToMove.id }
            updateGoalLists(oldSiblings.mapIndexed { index, list -> list.copy(order = index.toLong()) })
        }

        val newSiblings = (if (newParentId != null) {
            goalListDao.getListsByParentId(newParentId)
        } else {
            goalListDao.getTopLevelLists()
        }).filter { it.id != listToMove.id }

        val finalListToMove = listToMove.copy(order = newSiblings.size.toLong())
        goalListDao.update(finalListToMove)
    }

    // --- Інші публічні методи репозиторію ---

    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult> = goalDao.searchGoalsGlobal(query)
    fun getGoalsForListStream(listId: String): Flow<List<GoalWithInstanceInfo>> = goalDao.getGoalsForListStream(listId)
    suspend fun getGoalListById(id: String): GoalList? = goalListDao.getGoalListById(id)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun updateGoals(goals: List<Goal>) = goalDao.updateGoals(goals)
    fun getAssociatedListsForGoals(goalIds: List<String>): Flow<Map<String, List<GoalList>>> {
        if (goalIds.isEmpty()) return flowOf(emptyMap())
        val goalsFlow = goalDao.getGoalsByIds(goalIds)
        val allListsFlow = goalListDao.getAllLists()
        return combine(goalsFlow, allListsFlow) { goals, allLists ->
            val listLookup = allLists.associateBy { it.id }
            goals.associate { goal ->
                val associatedLists = goal.associatedListIds?.mapNotNull { listLookup[it] } ?: emptyList()
                goal.id to associatedLists
            }
        }
    }
    fun getAllGoalListsFlow(): Flow<List<GoalList>> = goalListDao.getAllLists()
    suspend fun getAllGoalLists(): List<GoalList> = goalListDao.getAll()
    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()
    suspend fun getAllGoalInstances(): List<GoalInstance> = goalDao.getAllInstances()
    suspend fun insertGoalLists(lists: List<GoalList>) = goalListDao.insertLists(lists)
    suspend fun insertGoals(goals: List<Goal>) = goalDao.insertGoals(goals)
    suspend fun deleteInstancesForLists(listIds: List<String>) = goalDao.deleteInstancesForLists(listIds)
    suspend fun insertGoalInstances(instances: List<GoalInstance>) = goalDao.insertGoalInstances(instances)
    suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)
    suspend fun getListsByIds(ids: List<String>): List<GoalList> = goalListDao.getListsByIds(ids)
    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun insertInstance(instance: GoalInstance) = goalDao.insertInstance(instance)
    fun getGoalListByIdFlow(id: String): Flow<GoalList?> = goalListDao.getGoalListByIdStream(id)
    suspend fun findListIdsByTag(tag: String): List<String> = goalListDao.getListsByTag(tag).map { it.id }
    suspend fun doesInstanceExist(goalId: String, listId: String): Boolean = goalDao.getInstanceCount(goalId, listId) > 0
    suspend fun deleteGoalInstanceByGoalIdAndListId(goalId: String, listId: String) = goalDao.deleteInstanceByGoalAndList(goalId, listId)
    suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal> = goalDao.getGoalsByIdsSuspend(ids)

    suspend fun createGoalListWithId(id: String, name: String, parentId: String?) {
        val newList = GoalList(
            id = id,
            name = name,
            parentId = parentId,
            description = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        goalListDao.insert(newList)
    }

    suspend fun logListAccess(listId: String) {
        recentListDao.logAccess(RecentListEntry(listId = listId, lastAccessed = System.currentTimeMillis()))
    }
    fun getRecentLists(limit: Int = 20): Flow<List<GoalList>> = recentListDao.getRecentLists(limit)

    suspend fun updateMarkdown(goalId: String, markdown: String) {
        goalDao.updateMarkdown(goalId, markdown)
    }

    fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()
}
