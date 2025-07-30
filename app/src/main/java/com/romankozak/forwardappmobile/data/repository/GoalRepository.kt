package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    // ✨ ЗМІНА: Метод тепер повертає ID створеного екземпляру (String).
    suspend fun createGoal(title: String, listId: String): String {
        val currentTime = System.currentTimeMillis()
        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            text = title,
            description = "",
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime,
            tags = null,
            // За замовчуванням, нові цілі не мають швидких посилань
            associatedListIds = emptyList()
        )

        val order = -currentTime

        val newInstance = GoalInstance(
            instanceId = UUID.randomUUID().toString(),
            goalId = newGoal.id,
            listId = listId,
            order = order
        )

        goalDao.insertGoalWithInstance(newGoal, newInstance)

        return newInstance.instanceId
    }

    suspend fun deleteGoalInstance(instanceId: String) {
        goalDao.deleteInstanceById(instanceId)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    // ✨ ЗМІНА: Повернуто до простої версії. Цей метод НЕ МАЄ чіпати Goal.associatedListIds.
    suspend fun createGoalInstance(goalId: String, targetListId: String) {
        val order = -System.currentTimeMillis()
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
        // Створюємо повну копію цілі, включаючи її "швидкі посилання"
        val newGoal = goal.copy(id = UUID.randomUUID().toString())
        goalDao.insertGoal(newGoal)
        // Створюємо екземпляр цієї нової цілі в цільовому списку
        createGoalInstance(newGoal.id, targetListId)
    }

    // ✨ ЗМІНА: Повністю переписана логіка для правильного отримання даних
    fun getAssociatedListsForGoals(goalIds: List<String>): Flow<Map<String, List<GoalList>>> {
        if (goalIds.isEmpty()) return flowOf(emptyMap())

        // 1. Отримуємо потік потрібних нам цілей
        val goalsFlow = goalDao.getGoalsByIds(goalIds)
        // 2. Отримуємо потік усіх списків (для пошуку)
        val allListsFlow = goalListDao.getAllLists()

        // 3. Комбінуємо два потоки
        return combine(goalsFlow, allListsFlow) { goals, allLists ->
            val listLookup = allLists.associateBy { it.id }
            val resultMap = mutableMapOf<String, List<GoalList>>()

            // 4. Для кожної цілі знаходимо її асоційовані списки
            for (goal in goals) {
                val associatedIds = goal.associatedListIds ?: emptyList()
                // Використовуємо `mapNotNull`, щоб безпечно отримати списки з нашого кешу `listLookup`
                val associatedLists = associatedIds.mapNotNull { listLookup[it] }
                resultMap[goal.id] = associatedLists
            }
            resultMap
        }
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

    // --- Методи для SyncRepository ---
    suspend fun getAllGoalLists(): List<GoalList> = goalListDao.getAll()
    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()
    suspend fun getAllGoalInstances(): List<GoalInstance> = goalDao.getAllInstances()
    suspend fun insertGoalLists(lists: List<GoalList>) = goalListDao.insertLists(lists)
    suspend fun insertGoals(goals: List<Goal>) = goalDao.insertGoals(goals)
    suspend fun deleteInstancesForLists(listIds: List<String>) = goalDao.deleteInstancesForLists(listIds)
    suspend fun insertGoalInstances(instances: List<GoalInstance>) = goalDao.insertGoalInstances(instances)
    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult> = goalDao.searchGoalsGlobal(query)

    // --- Методи для GoalEditViewModel ---
    suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)
    suspend fun getListsByIds(ids: List<String>): List<GoalList> = goalListDao.getListsByIds(ids)
    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun getGoalCountInList(listId: String): Int = goalDao.getGoalCountInList(listId)
    suspend fun insertInstance(instance: GoalInstance) = goalDao.insertInstance(instance)
    fun getGoalListByIdFlow(id: String): Flow<GoalList?> = goalListDao.getGoalListByIdStream(id)

    suspend fun updateListsOrder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, listId ->
            goalListDao.updateOrder(listId, index.toLong())
        }
    }
}