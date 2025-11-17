package com.romankozak.forwardappmobile.shared.features.settings.logic

import com.romankozak.forwardappmobile.shared.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ContextHandler(
    private val settingsRepository: SettingsRepository,
    private val iconProvider: IconProvider,
) {
    private val _contextMarkerToEmojiMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = _contextMarkerToEmojiMap.asStateFlow()

    suspend fun initialize() {
        loadContextSettings()
    }

    private suspend fun loadContextSettings() = coroutineScope {
        launch {
            val localMarkerToEmojiMap = mutableMapOf<String, String>()
            val hardcodedIconsData = iconProvider.getIconMappings()
            hardcodedIconsData.forEach { (icon, markers) ->
                markers.forEach { marker ->
                    localMarkerToEmojiMap[marker] = icon
                }
            }

            val reservedContextsInfo = SettingsRepository.reservedContexts.map { (name, keys) ->
                Triple(name, keys.first, keys.second)
            }

            reservedContextsInfo.forEach { (name, tagKey, emojiKey) ->
                val emoji = settingsRepository.getContextEmojiFlow(emojiKey).first()
                val marker = "@${name.lowercase()}"
                if (emoji.isNotBlank()) localMarkerToEmojiMap[marker] = emoji
            }

            val customNames = settingsRepository.customContextNamesFlow.first()
            customNames.forEach { name ->
                val emoji = settingsRepository.getCustomContextEmojiFlow(name).first()
                val marker = "@${name.lowercase()}"
                if (emoji.isNotBlank()) localMarkerToEmojiMap[marker] = emoji
            }

            _contextMarkerToEmojiMap.value = localMarkerToEmojiMap
        }
    }
}
