package com.romankozak.forwardappmobile.data.logic

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextHandler @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository
) {
    private val contextTagMap = mutableMapOf<String, String>()
    private var isInitialized = false
    private val _contextNamesFlow = MutableStateFlow<List<String>>(emptyList())
    val contextNamesFlow: StateFlow<List<String>> = _contextNamesFlow.asStateFlow()

    suspend fun initialize() {
        if (isInitialized) return
        loadContextSettings()
        isInitialized = true
    }

    private suspend fun loadContextSettings() {
        Log.d("ContextDebug", "ContextHandler: Starting to load context settings...")
        val contextKeysList = listOf(
            SettingsRepository.ContextKeys.BUY,
            SettingsRepository.ContextKeys.PM,
            SettingsRepository.ContextKeys.PAPER,
            SettingsRepository.ContextKeys.MENTAL,
            SettingsRepository.ContextKeys.PROVIDENCE,
            SettingsRepository.ContextKeys.MANUAL,
            SettingsRepository.ContextKeys.RESEARCH,
            SettingsRepository.ContextKeys.DEVICE,
            SettingsRepository.ContextKeys.MIDDLE, // <-- Додано
            SettingsRepository.ContextKeys.LONG    // <-- Додано
        )

        coroutineScope {
            val deferreds = contextKeysList.map { key ->
                async(Dispatchers.IO) {
                    try {
                        val contextName = key.name.removePrefix("context_tag_")
                        val tag = settingsRepository.getContextTagFlow(key).first()
                        contextTagMap[contextName] = tag
                    } catch (e: Exception) {
                        Log.e("ContextDebug", "Error loading a context setting for key ${key.name}", e)
                    }
                }
            }
            deferreds.awaitAll()
        }
        _contextNamesFlow.value = contextTagMap.keys.sorted()
        Log.d("ContextDebug", "ContextHandler: Finished loading settings. Final map: $contextTagMap")
    }

    private fun parseContextsFromText(text: String): Set<String> {
        val regex = "@\\{?(\\w+)\\}?".toRegex()
        return regex.findAll(text).map { it.groupValues[1] }.toSet()
    }

    // ✨ НОВИЙ приватний метод, що перевіряє та створює екземпляри
    private suspend fun ensureInstancesExist(goal: Goal, contexts: Set<String>) {
        contexts.forEach { contextName ->
            val tag = contextTagMap[contextName]
            if (tag != null) {
                val targetListIds = goalRepository.findListIdsByTag(tag)
                targetListIds.forEach { listId ->
                    val exists = goalRepository.doesInstanceExist(goal.id, listId)
                    if (!exists) {
                        Log.d("ContextDebug", "CONSISTENCY CHECK: Instance for context '$contextName' in list $listId is missing. Creating it.")
                        val order = goalRepository.getGoalCountInList(listId).toLong()
                        val newInstance = GoalInstance(
                            instanceId = UUID.randomUUID().toString(),
                            goalId = goal.id,
                            listId = listId,
                            order = order
                        )
                        goalRepository.insertInstance(newInstance)
                    }
                }
            }
        }
    }

    // Метод для створення НОВИХ цілей
    suspend fun handleContextsOnCreate(goal: Goal) {
        if (!isInitialized) initialize()
        val contexts = parseContextsFromText(goal.text)
        Log.d("ContextDebug", "Handling context CREATION for goal: '${goal.text}', contexts: $contexts")
        if (contexts.isNotEmpty()) {
            ensureInstancesExist(goal, contexts)
        }
    }

    // Метод для ОНОВЛЕННЯ існуючих цілей
    suspend fun syncContextsOnUpdate(oldGoal: Goal, newGoal: Goal) {
        if (!isInitialized) initialize()

        val oldContexts = parseContextsFromText(oldGoal.text)
        val newContexts = parseContextsFromText(newGoal.text)

        // Крок 1: Видаляємо екземпляри для контекстів, які зникли
        val contextsToRemove = oldContexts - newContexts
        Log.d("ContextDebug", "Syncing... To remove: $contextsToRemove")
        contextsToRemove.forEach { contextName ->
            val tag = contextTagMap[contextName]
            if (tag != null) {
                val targetListIds = goalRepository.findListIdsByTag(tag)
                targetListIds.forEach { listId ->
                    Log.d("ContextDebug", "Removing instance for removed context '$contextName' from list $listId")
                    goalRepository.deleteGoalInstanceByGoalIdAndListId(goalId = oldGoal.id, listId = listId)
                }
            }
        }

        // ✨ Крок 2 (ОНОВЛЕНО): Перевіряємо узгодженість для ВСІХ контекстів, що залишились
        Log.d("ContextDebug", "Syncing... Ensuring final state for contexts: $newContexts")
        if (newContexts.isNotEmpty()) {
            ensureInstancesExist(newGoal, newContexts)
        }
    }
}