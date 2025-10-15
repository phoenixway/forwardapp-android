
package com.romankozak.forwardappmobile.data.logic

import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
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
class ContextHandler
    @Inject
    constructor(
        private val projectRepositoryProvider: Provider<ProjectRepository>,
        private val settingsRepository: SettingsRepository,
        private val goalRepositoryProvider: Provider<com.romankozak.forwardappmobile.data.repository.GoalRepository>,
    ) {
        private val projectRepository: ProjectRepository by lazy { projectRepositoryProvider.get() }
        private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository by lazy { goalRepositoryProvider.get() }

        private val contextTagMap = mutableMapOf<String, String>()
        private val _contextNamesFlow = MutableStateFlow<List<String>>(emptyList())
        val contextNamesFlow: StateFlow<List<String>> = _contextNamesFlow.asStateFlow()

        private val _tagToContextNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
        val tagToContextNameMap: StateFlow<Map<String, String>> = _tagToContextNameMap.asStateFlow()

        private val _contextMarkerToEmojiMap = MutableStateFlow<Map<String, String>>(emptyMap())
        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = _contextMarkerToEmojiMap.asStateFlow()

        private val _allContextsFlow = MutableStateFlow<List<UiContext>>(emptyList())
        val allContextsFlow: StateFlow<List<UiContext>> = _allContextsFlow.asStateFlow()

        private val contextMarkerMap = mutableMapOf<String, String>()

        fun getContextMarker(contextName: String): String? = contextMarkerMap[contextName.uppercase()]

        fun getContextTag(contextName: String): String? = contextTagMap[contextName.lowercase()]

        suspend fun initialize() {
            loadContextSettings()
        }

        private suspend fun loadContextSettings() {
            val localContextTagMap = mutableMapOf<String, String>()
            val localContextMarkerMap = mutableMapOf<String, String>()
            val localMarkerToEmojiMap = mutableMapOf<String, String>()
            val contextsBeingBuilt = mutableListOf<UiContext>()

            val reservedContextsInfo =
                listOf(
                    Triple("BUY", SettingsRepository.ContextKeys.BUY, SettingsRepository.ContextKeys.EMOJI_BUY),
                    Triple("PM", SettingsRepository.ContextKeys.PM, SettingsRepository.ContextKeys.EMOJI_PM),
                    Triple("PAPER", SettingsRepository.ContextKeys.PAPER, SettingsRepository.ContextKeys.EMOJI_PAPER),
                    Triple("MENTAL", SettingsRepository.ContextKeys.MENTAL, SettingsRepository.ContextKeys.EMOJI_MENTAL),
                    Triple("PROVIDENCE", SettingsRepository.ContextKeys.PROVIDENCE, SettingsRepository.ContextKeys.EMOJI_PROVIDENCE),
                    Triple("MANUAL", SettingsRepository.ContextKeys.MANUAL, SettingsRepository.ContextKeys.EMOJI_MANUAL),
                    Triple("RESEARCH", SettingsRepository.ContextKeys.RESEARCH, SettingsRepository.ContextKeys.EMOJI_RESEARCH),
                    Triple("DEVICE", SettingsRepository.ContextKeys.DEVICE, SettingsRepository.ContextKeys.EMOJI_DEVICE),
                    Triple("MIDDLE", SettingsRepository.ContextKeys.MIDDLE, SettingsRepository.ContextKeys.EMOJI_MIDDLE),
                    Triple("LONG", SettingsRepository.ContextKeys.LONG, SettingsRepository.ContextKeys.EMOJI_LONG),
                )

            reservedContextsInfo.forEach { (name, tagKey, emojiKey) ->
                val tag = settingsRepository.getContextTagFlow(tagKey).first()
                val emoji = settingsRepository.getContextEmojiFlow(emojiKey).first()
                val marker = "@${name.lowercase()}"

                localContextTagMap[name.lowercase()] = tag
                localContextMarkerMap[name.uppercase()] = marker
                if (emoji.isNotBlank()) localMarkerToEmojiMap[marker] = emoji

                contextsBeingBuilt.add(
                    UiContext(
                        name = name.lowercase(),
                        emoji = emoji,
                        tag = tag,
                        isReserved = true,
                    ),
                )
            }

            val customNames = settingsRepository.customContextNamesFlow.first()
            customNames.forEach { name ->
                val tag = settingsRepository.getCustomContextTagFlow(name).first()
                val emoji = settingsRepository.getCustomContextEmojiFlow(name).first()
                if (tag.isNotBlank()) {
                    val marker = "@${name.lowercase()}"
                    localContextTagMap[name.lowercase()] = tag
                    localContextMarkerMap[name.uppercase()] = marker
                    if (emoji.isNotBlank()) localMarkerToEmojiMap[marker] = emoji

                    contextsBeingBuilt.add(
                        UiContext(
                            name = name.lowercase(),
                            emoji = emoji,
                            tag = tag,
                            isReserved = false,
                        ),
                    )
                }
            }

            _allContextsFlow.value = contextsBeingBuilt.sortedBy { it.name }

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
            return regex.findAll(text).map { it.groupValues[1].lowercase() }.toSet()
        }

        private suspend fun ensureLinksExist(
            goal: Goal,
            contexts: Set<String>,
        ) = coroutineScope {
            contexts
                .map { contextName ->
                    async {
                        val tag = contextTagMap[contextName.lowercase()]
                        if (tag != null) {
                            val targetProjectIds = projectRepository.findProjectIdsByTag(tag)
                            for (projectId in targetProjectIds) {
                                if (!projectRepository.doesLinkExist(goal.id, projectId)) {
                                    goalRepository.createGoalLinks(listOf(goal.id), projectId)
                                }
                            }
                        }
                    }
                }.awaitAll()
        }

        suspend fun handleContextsOnCreate(goal: Goal) {
            val contexts = parseContextsFromText(goal.text)
            if (contexts.isNotEmpty()) {
                ensureLinksExist(goal, contexts)
            }
        }

        suspend fun syncContextsOnUpdate(
            oldGoal: Goal,
            newGoal: Goal,
        ) = coroutineScope {
            val oldContexts = parseContextsFromText(oldGoal.text)
            val newContexts = parseContextsFromText(newGoal.text)

            if (oldContexts == newContexts) return@coroutineScope

            val contextsToRemove = oldContexts - newContexts
            val removalJobs =
                contextsToRemove.map { contextName ->
                    async {
                        val tag = contextTagMap[contextName.lowercase()]
                        if (tag != null) {
                            val targetProjectIds = projectRepository.findProjectIdsByTag(tag)
                            for (projectId in targetProjectIds) {
                                projectRepository.deleteLinkByEntityIdAndProjectId(entityId = oldGoal.id, projectId = projectId)
                            }
                        }
                    }
                }

            val contextsToAdd = newContexts - oldContexts
            if (contextsToAdd.isNotEmpty()) {
                ensureLinksExist(newGoal, contextsToAdd)
            }

            removalJobs.awaitAll()
        }
    }
