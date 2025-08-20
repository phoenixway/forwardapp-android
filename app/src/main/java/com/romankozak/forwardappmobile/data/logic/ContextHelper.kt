// Файл: app/src/main/java/com/romankozak/forwardappmobile/data/logic/ContextHelper.kt

package com.romankozak.forwardappmobile.data.logic

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.Goal
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
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ContextHandler @Inject constructor(
    private val goalRepositoryProvider: Provider<GoalRepository>,
    private val settingsRepository: SettingsRepository
) {
    private val goalRepository: GoalRepository by lazy { goalRepositoryProvider.get() }

    private val contextTagMap = mutableMapOf<String, String>()
    private var isInitialized = false
    private val _contextNamesFlow = MutableStateFlow<List<String>>(emptyList())
    val contextNamesFlow: StateFlow<List<String>> = _contextNamesFlow.asStateFlow()

    private val _tagToContextNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagToContextNameMap: StateFlow<Map<String, String>> = _tagToContextNameMap.asStateFlow()

    private val contextMarkerMap = mapOf(
        "BUY" to "@buy", "PM" to "@pm", "PAPER" to "@paper", "MENTAL" to "@mental",
        "PROVIDENCE" to "@providence", "MANUAL" to "@manual", "RESEARCH" to "@research",
        "DEVICE" to "@device", "MIDDLE" to "@middle", "LONG" to "@long"
    )

    fun getContextMarker(contextName: String): String? {
        val key = contextMarkerMap.keys.find { it.equals(contextName, ignoreCase = true) }
        return key?.let { contextMarkerMap[it] }
    }

    // ✨ ДОДАНО: Повертаємо загублений метод
    fun getContextTag(contextName: String): String? {
        if (!isInitialized) {
            Log.w("ContextDebug", "getContextTag called before handler was initialized!")
        }
        // Ключі в contextTagMap зберігаються у нижньому регістрі (e.g., "buy", "pm")
        return contextTagMap[contextName.lowercase()]
    }

    suspend fun initialize() {
        if (isInitialized) return
        loadContextSettings()
        isInitialized = true
    }

    private suspend fun loadContextSettings() {
        val contextKeysList = listOf(
            SettingsRepository.ContextKeys.BUY, SettingsRepository.ContextKeys.PM,
            SettingsRepository.ContextKeys.PAPER, SettingsRepository.ContextKeys.MENTAL,
            SettingsRepository.ContextKeys.PROVIDENCE, SettingsRepository.ContextKeys.MANUAL,
            SettingsRepository.ContextKeys.RESEARCH, SettingsRepository.ContextKeys.DEVICE,
            SettingsRepository.ContextKeys.MIDDLE, SettingsRepository.ContextKeys.LONG
        )
        coroutineScope {
            val deferreds = contextKeysList.map { key ->
                async(Dispatchers.IO) {
                    try {
                        // Приводимо до нижнього регістру для консистентності
                        val contextName = key.name.removePrefix("context_tag_").lowercase()
                        val tag = settingsRepository.getContextTagFlow(key).first()
                        contextTagMap[contextName] = tag
                    } catch (e: Exception) {
                        Log.e("ContextDebug", "Error loading context for key ${key.name}", e)
                    }
                }
            }
            deferreds.awaitAll()
        }
        _contextNamesFlow.value = contextTagMap.keys.sorted()
        _tagToContextNameMap.value = contextTagMap.entries.associate { (k, v) -> v to k }
    }

    private fun parseContextsFromText(text: String): Set<String> {
        val regex = "@\\{?(\\w+)\\}?".toRegex()
        return regex.findAll(text).map { it.groupValues[1] }.toSet()
    }

    private suspend fun ensureInstancesExist(goal: Goal, contexts: Set<String>) {
        contexts.forEach { contextName ->
            val tag = contextTagMap[contextName.lowercase()]
            if (tag != null) {
                val targetListIds = goalRepository.findListIdsByTag(tag)
                targetListIds.forEach { listId ->
                    if (!goalRepository.doesInstanceExist(goal.id, listId)) {
                        goalRepository.createGoalInstances(listOf(goal.id), listId)
                    }
                }
            }
        }
    }

    suspend fun handleContextsOnCreate(goal: Goal) {
        if (!isInitialized) initialize()
        val contexts = parseContextsFromText(goal.text)
        if (contexts.isNotEmpty()) {
            ensureInstancesExist(goal, contexts)
        }
    }

    suspend fun syncContextsOnUpdate(oldGoal: Goal, newGoal: Goal) {
        if (!isInitialized) initialize()
        val oldContexts = parseContextsFromText(oldGoal.text)
        val newContexts = parseContextsFromText(newGoal.text)

        val contextsToRemove = oldContexts - newContexts
        contextsToRemove.forEach { contextName ->
            val tag = contextTagMap[contextName.lowercase()]
            if (tag != null) {
                val targetListIds = goalRepository.findListIdsByTag(tag)
                targetListIds.forEach { listId ->
                    goalRepository.deleteGoalInstanceByGoalIdAndListId(goalId = oldGoal.id, listId = listId)
                }
            }
        }
        if (newContexts.isNotEmpty()) {
            ensureInstancesExist(newGoal, newContexts)
        }
    }
}
