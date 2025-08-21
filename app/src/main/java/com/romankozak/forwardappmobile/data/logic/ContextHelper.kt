// Файл: app/src/main/java/com/romankozak/forwardappmobile/data/logic/ContextHelper.kt

package com.romankozak.forwardappmobile.data.logic

import android.util.Log
import androidx.datastore.preferences.core.Preferences
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
    private val _contextNamesFlow = MutableStateFlow<List<String>>(emptyList())
    val contextNamesFlow: StateFlow<List<String>> = _contextNamesFlow.asStateFlow()

    private val _tagToContextNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagToContextNameMap: StateFlow<Map<String, String>> = _tagToContextNameMap.asStateFlow()

    private val _contextMarkerToEmojiMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = _contextMarkerToEmojiMap.asStateFlow()

    private val contextMarkerMap = mutableMapOf(
        "BUY" to "@buy", "PM" to "@pm", "PAPER" to "@paper", "MENTAL" to "@mental",
        "PROVIDENCE" to "@providence", "MANUAL" to "@manual", "RESEARCH" to "@research",
        "DEVICE" to "@device", "MIDDLE" to "@middle", "LONG" to "@long"
    )

    fun getContextMarker(contextName: String): String? {
        val key = contextMarkerMap.keys.find { it.equals(contextName, ignoreCase = true) }
        return key?.let { contextMarkerMap[it] }
    }

    fun getContextTag(contextName: String): String? {
        return contextTagMap[contextName.lowercase()]
    }

    // ✨ ВИПРАВЛЕНО: Метод тепер може викликатися повторно для оновлення налаштувань
    suspend fun initialize() {
        // Просто викликаємо завантаження налаштувань.
        // Більше ніяких очищень чи retainAll тут не потрібно.
        loadContextSettings()
    }


    private suspend fun loadContextSettings() {
        // Створюємо тимчасові карти, щоб уникнути проблем з багатопоточністю
        // та гарантувати, що дані будуть консистентними.
        val localContextTagMap = mutableMapOf<String, String>()
        val localContextMarkerMap = mutableMapOf(
            "BUY" to "@buy", "PM" to "@pm", "PAPER" to "@paper", "MENTAL" to "@mental",
            "PROVIDENCE" to "@providence", "MANUAL" to "@manual", "RESEARCH" to "@research",
            "DEVICE" to "@device", "MIDDLE" to "@middle", "LONG" to "@long"
        )
        val localMarkerToEmojiMap = mutableMapOf<String, String>()

        val reservedContextsInfo = listOf(
            Triple("BUY", SettingsRepository.ContextKeys.BUY, SettingsRepository.ContextKeys.EMOJI_BUY),
            Triple("PM", SettingsRepository.ContextKeys.PM, SettingsRepository.ContextKeys.EMOJI_PM),
            Triple("PAPER", SettingsRepository.ContextKeys.PAPER, SettingsRepository.ContextKeys.EMOJI_PAPER),
            Triple("MENTAL", SettingsRepository.ContextKeys.MENTAL, SettingsRepository.ContextKeys.EMOJI_MENTAL),
            Triple("PROVIDENCE", SettingsRepository.ContextKeys.PROVIDENCE, SettingsRepository.ContextKeys.EMOJI_PROVIDENCE),
            Triple("MANUAL", SettingsRepository.ContextKeys.MANUAL, SettingsRepository.ContextKeys.EMOJI_MANUAL),
            Triple("RESEARCH", SettingsRepository.ContextKeys.RESEARCH, SettingsRepository.ContextKeys.EMOJI_RESEARCH),
            Triple("DEVICE", SettingsRepository.ContextKeys.DEVICE, SettingsRepository.ContextKeys.EMOJI_DEVICE),
            Triple("MIDDLE", SettingsRepository.ContextKeys.MIDDLE, SettingsRepository.ContextKeys.EMOJI_MIDDLE),
            Triple("LONG", SettingsRepository.ContextKeys.LONG, SettingsRepository.ContextKeys.EMOJI_LONG)
        )

        reservedContextsInfo.forEach { (name, tagKey, emojiKey) ->
            try {
                val tag = settingsRepository.getContextTagFlow(tagKey).first()
                val emoji = settingsRepository.getContextEmojiFlow(emojiKey).first()
                localContextTagMap[name.lowercase()] = tag
                localContextMarkerMap[name.uppercase()]?.let { marker ->
                    if (emoji.isNotBlank()) localMarkerToEmojiMap[marker] = emoji
                }
            } catch (e: Exception) {
                Log.e("ContextDebug", "Error loading reserved context '$name'", e)
            }
        }

        val customNames = settingsRepository.customContextNamesFlow.first()
        customNames.forEach { name ->
            try {
                val tag = settingsRepository.getCustomContextTagFlow(name).first()
                val emoji = settingsRepository.getCustomContextEmojiFlow(name).first()
                if (tag.isNotBlank()) {
                    val marker = "@${name.lowercase()}"
                    localContextTagMap[name.lowercase()] = tag
                    localContextMarkerMap[name.uppercase()] = marker
                    if (emoji.isNotBlank()) localMarkerToEmojiMap[marker] = emoji
                }
            } catch (e: Exception) {
                Log.e("ContextDebug", "Error loading custom context '$name'", e)
            }
        }

        // Тепер, коли всі дані зібрані, атомарно оновлюємо основні змінні класу
        contextTagMap.clear()
        contextTagMap.putAll(localContextTagMap)

        contextMarkerMap.clear()
        contextMarkerMap.putAll(localContextMarkerMap)

        _contextNamesFlow.value = localContextTagMap.keys.sorted()
        _tagToContextNameMap.value = localContextTagMap.entries.associate { (k, v) -> v to k }
        _contextMarkerToEmojiMap.value = localMarkerToEmojiMap
    }

    private fun parseContextsFromText(text: String): Set<String> {
        val regex = "@\\{?([\\w-]+)\\}?".toRegex()
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
        initialize() // Ensure settings are loaded
        val contexts = parseContextsFromText(goal.text)
        if (contexts.isNotEmpty()) {
            ensureInstancesExist(goal, contexts)
        }
    }

    suspend fun syncContextsOnUpdate(oldGoal: Goal, newGoal: Goal) {
        initialize() // Ensure settings are loaded
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