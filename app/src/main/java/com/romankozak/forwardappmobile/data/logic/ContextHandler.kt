package com.romankozak.forwardappmobile.data.logic

import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.common.IconProvider
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ContextHandler @Inject constructor(
    private val contextRepositoryProvider: Provider<ContextRepository>,
    private val settingsRepository: SettingsRepository,
    private val goalRepositoryProvider: Provider<GoalRepository>,
    private val iconProvider: IconProvider,
) {
    private val contextRepository: ContextRepository by lazy { contextRepositoryProvider.get() }
    private val goalRepository: GoalRepository by lazy { goalRepositoryProvider.get() }

    private val contextTagMap = mutableMapOf<String, String>()
    private val contextMarkerMap = mutableMapOf<String, String>()

    private val _allContextsFlow = MutableStateFlow<List<UiContext>>(emptyList())
    val allContextsFlow: StateFlow<List<UiContext>> = _allContextsFlow.asStateFlow()

    private val _tagToContextNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagToContextNameMap: StateFlow<Map<String, String>> = _tagToContextNameMap.asStateFlow()

    fun getContextMarker(contextName: String): String? = contextMarkerMap[contextName.uppercase()]

    suspend fun initialize() {
        loadContextSettings()
    }


    private fun parseContextsFromText(text: String): Set<String> {
        val regex = "@\\{?([\\w-]+)\\}?".toRegex()
        return regex.findAll(text).map { it.groupValues[1].lowercase() }.toSet()
    }

    private suspend fun loadContextSettings() {
        val localContextTagMap = mutableMapOf<String, String>()
        val localContextMarkerMap = mutableMapOf<String, String>()
        val localMarkerToEmojiMap = mutableMapOf<String, String>()
        val contextsBeingBuilt = mutableListOf<UiContext>()

        iconProvider.getIconMappings().forEach { (icon, markers) ->
            markers.forEach { marker -> localMarkerToEmojiMap[marker] = icon }
        }

        // Зарезервовані
        SettingsRepository.ContextKeys.reservedContexts.forEach { (name, keys) ->
            val tag = settingsRepository.getContextTagFlow(keys.first).first()
            val emojiValue = settingsRepository.getContextEmojiFlow(keys.second).first()
            val marker = "@${name.lowercase()}"
            localContextTagMap[name.lowercase()] = tag
            localContextMarkerMap[name.uppercase()] = marker
            if (emojiValue.isNotBlank()) localMarkerToEmojiMap[marker] = emojiValue

            contextsBeingBuilt.add(UiContext(name = name.lowercase(), emoji = emojiValue, tag = tag, isReserved = true))
        }

        // Кастомні
        val customNames = settingsRepository.customContextNamesFlow.first()
        customNames.forEach { name ->
            val tag = settingsRepository.getCustomContextTagFlow(name).first()
            val emojiValue = settingsRepository.getCustomContextEmojiFlow(name).first()
            if (tag.isNotBlank()) {
                val marker = "@${name.lowercase()}"
                localContextTagMap[name.lowercase()] = tag
                localContextMarkerMap[name.uppercase()] = marker
                if (emojiValue.isNotBlank()) localMarkerToEmojiMap[marker] = emojiValue
                contextsBeingBuilt.add(UiContext(name = name.lowercase(), emoji = emojiValue, tag = tag, isReserved = false))
            }
        }

        _allContextsFlow.value = contextsBeingBuilt.sortedBy { it.name }
        contextTagMap.apply { clear(); putAll(localContextTagMap) }
        contextMarkerMap.apply { clear(); putAll(localContextMarkerMap) }
        _tagToContextNameMap.value = localContextTagMap.entries.associate { it.value to it.key }
    }

    private suspend fun ensureLinksExist(goal: Goal, contexts: Set<String>) = coroutineScope {
        contexts.map { contextName ->
            async {
                val tag = contextTagMap[contextName.lowercase()]
                if (tag != null) {
                    val targetContextIds = contextRepository.findContextIdsByTag(tag)
                    for (contextId in targetContextIds) {
                        if (!contextRepository.doesLinkToContextExist(goal.id, contextId)) {
                            goalRepository.createGoalLinks(listOf(goal.id), contextId)
                        }
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun handleContextsOnCreate(goal: Goal) {
        val contexts = parseContextsFromText(goal.text)
        if (contexts.isNotEmpty()) ensureLinksExist(goal, contexts)
    }

    suspend fun syncContextsOnUpdate(oldGoal: Goal, newGoal: Goal) = coroutineScope {
        val oldContexts = parseContextsFromText(oldGoal.text)
        val newContexts = parseContextsFromText(newGoal.text)
        if (oldContexts == newContexts) return@coroutineScope

        val contextsToRemove = oldContexts - newContexts
        contextsToRemove.map { contextName ->
            async {
                val tag = contextTagMap[contextName.lowercase()]
                if (tag != null) {
                    val targetContextIds = contextRepository.findContextIdsByTag(tag)
                    for (contextId in targetContextIds) {
                        contextRepository.deleteLinkByEntityIdAndContextId(oldGoal.id, contextId)
                    }
                }
            }
        }.awaitAll()

        val contextsToAdd = newContexts - oldContexts
        if (contextsToAdd.isNotEmpty()) ensureLinksExist(newGoal, contextsToAdd)
    }
}