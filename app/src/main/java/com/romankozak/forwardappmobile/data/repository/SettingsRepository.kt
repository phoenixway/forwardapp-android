package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val desktopAddressKey = stringPreferencesKey("desktop_address")
        private val obsidianVaultNameKey = stringPreferencesKey("obsidian_vault_name")
        private val showPlanningModesKey = booleanPreferencesKey("show_planning_modes")
        private val dailyTagKey = stringPreferencesKey("daily_planning_tag")
        private val mediumTagKey = stringPreferencesKey("medium_planning_tag")
        private val longTagKey = stringPreferencesKey("long_planning_tag")

        companion object {
            val OLLAMA_URL_KEY = stringPreferencesKey("ollama_url")
            val OLLAMA_FAST_MODEL_KEY = stringPreferencesKey("ollama_fast_model")
            val OLLAMA_SMART_MODEL_KEY = stringPreferencesKey("ollama_smart_model")
            val SYSTEM_PROMPT_KEY = stringPreferencesKey("system_prompt")

            val ROLE_TITLE_KEY = stringPreferencesKey("role_title")
            val TEMPERATURE_KEY = floatPreferencesKey("temperature")

            val ROLES_FOLDER_URI_KEY = stringPreferencesKey("roles_folder_uri")

            val NER_MODEL_URI_KEY = stringPreferencesKey("ner_model_uri")
            val NER_TOKENIZER_URI_KEY = stringPreferencesKey("ner_tokenizer_uri")
            val NER_LABELS_URI_KEY = stringPreferencesKey("ner_labels_uri")
        }

        object ContextKeys {
            val BUY = stringPreferencesKey("context_tag_buy")
            val PM = stringPreferencesKey("context_tag_pm")
            val PAPER = stringPreferencesKey("context_tag_paper")
            val MENTAL = stringPreferencesKey("context_tag_mental")
            val PROVIDENCE = stringPreferencesKey("context_tag_providence")
            val MANUAL = stringPreferencesKey("context_tag_manual")
            val RESEARCH = stringPreferencesKey("context_tag_research")
            val DEVICE = stringPreferencesKey("context_tag_device")
            val MIDDLE = stringPreferencesKey("context_tag_middle")
            val LONG = stringPreferencesKey("context_tag_long")

            val EMOJI_BUY = stringPreferencesKey("context_emoji_buy")
            val EMOJI_PM = stringPreferencesKey("context_emoji_pm")
            val EMOJI_PAPER = stringPreferencesKey("context_emoji_paper")
            val EMOJI_MENTAL = stringPreferencesKey("context_emoji_mental")
            val EMOJI_PROVIDENCE = stringPreferencesKey("context_emoji_providence")
            val EMOJI_MANUAL = stringPreferencesKey("context_emoji_manual")
            val EMOJI_RESEARCH = stringPreferencesKey("context_emoji_research")
            val EMOJI_DEVICE = stringPreferencesKey("context_emoji_device")
            val EMOJI_MIDDLE = stringPreferencesKey("context_emoji_middle")
            val EMOJI_LONG = stringPreferencesKey("context_emoji_long")

            val CUSTOM_CONTEXT_NAMES = stringSetPreferencesKey("custom_context_names")
        }

        val desktopAddressFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[desktopAddressKey] ?: "" }

        suspend fun saveDesktopAddress(address: String) {
            context.dataStore.edit { settings -> settings[desktopAddressKey] = address }
        }

        val obsidianVaultNameFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[obsidianVaultNameKey] ?: "" }

        suspend fun saveObsidianVaultName(name: String) {
            context.dataStore.edit { settings -> settings[obsidianVaultNameKey] = name }
        }

        val showPlanningModesFlow: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[showPlanningModesKey] ?: false }

        suspend fun saveShowPlanningModes(show: Boolean) {
            context.dataStore.edit { settings -> settings[showPlanningModesKey] = show }
        }

        val dailyTagFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[dailyTagKey] ?: "daily" }

        suspend fun saveDailyTag(tag: String) {
            context.dataStore.edit { settings -> settings[dailyTagKey] = tag }
        }

        val mediumTagFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[mediumTagKey] ?: "medium" }

        suspend fun saveMediumTag(tag: String) {
            context.dataStore.edit { settings -> settings[mediumTagKey] = tag }
        }

        val longTagFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[longTagKey] ?: "long" }

        suspend fun saveLongTag(tag: String) {
            context.dataStore.edit { settings -> settings[longTagKey] = tag }
        }

        val ollamaUrlFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[OLLAMA_URL_KEY] ?: "http://10.0.2.2:11434" }

        val ollamaFastModelFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[OLLAMA_FAST_MODEL_KEY] ?: "" }

        val ollamaSmartModelFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[OLLAMA_SMART_MODEL_KEY] ?: "" }

        val systemPromptFlow: Flow<String> =
            context.dataStore.data
                .map { preferences ->
                    preferences[SYSTEM_PROMPT_KEY] ?: "You are a helpful assistant who answers concisely and accurately."
                }

        suspend fun setSystemPrompt(prompt: String) {
            context.dataStore.edit { settings ->
                settings[SYSTEM_PROMPT_KEY] = prompt
            }
        }

        val roleTitleFlow: Flow<String> =
            context.dataStore.data
                .map { preferences ->
                    preferences[ROLE_TITLE_KEY] ?: "Assistant"
                }

        suspend fun setRoleTitle(title: String) {
            context.dataStore.edit { settings ->
                settings[ROLE_TITLE_KEY] = title
            }
        }

        val temperatureFlow: Flow<Float> =
            context.dataStore.data
                .map { preferences ->
                    try {
                        preferences[TEMPERATURE_KEY] ?: 0.8f
                    } catch (e: ClassCastException) {
                        Log.e("SettingsRepository", "Failed to cast temperature, resetting to default.", e)
                        (preferences[stringPreferencesKey(TEMPERATURE_KEY.name)]?.toFloatOrNull()) ?: 0.8f
                    }
                }

        suspend fun setTemperature(temperature: Float) {
            context.dataStore.edit { settings ->
                settings[TEMPERATURE_KEY] = temperature
            }
        }

        suspend fun saveOllamaUrl(url: String) {
            context.dataStore.edit { settings -> settings[OLLAMA_URL_KEY] = url }
        }

        suspend fun saveOllamaModels(
            fastModel: String,
            smartModel: String,
        ) {
            context.dataStore.edit { settings ->
                settings[OLLAMA_FAST_MODEL_KEY] = fastModel
                settings[OLLAMA_SMART_MODEL_KEY] = smartModel
            }
        }

        val nerModelUriFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[NER_MODEL_URI_KEY] ?: "" }

        val nerTokenizerUriFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[NER_TOKENIZER_URI_KEY] ?: "" }

        val nerLabelsUriFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[NER_LABELS_URI_KEY] ?: "" }

        suspend fun saveNerUris(
            modelUri: String,
            tokenizerUri: String,
            labelsUri: String,
        ) {
            context.dataStore.edit { settings ->
                settings[NER_MODEL_URI_KEY] = modelUri
                settings[NER_TOKENIZER_URI_KEY] = tokenizerUri
                settings[NER_LABELS_URI_KEY] = labelsUri
            }
        }

        fun getContextTagFlow(contextKey: Preferences.Key<String>): Flow<String> =
            context.dataStore.data.map { preferences ->
                val contextName = contextKey.name.removePrefix("context_tag_")
                val savedTag = preferences[contextKey]
                if (savedTag.isNullOrBlank()) {
                    "${contextName}_context_tag"
                } else {
                    savedTag
                }
            }

        suspend fun saveContextTag(
            contextKey: Preferences.Key<String>,
            tag: String,
        ) {
            context.dataStore.edit { settings -> settings[contextKey] = tag }
        }

        fun getContextEmojiFlow(emojiKey: Preferences.Key<String>): Flow<String> =
            context.dataStore.data.map { preferences ->
                preferences[emojiKey] ?: ""
            }

        suspend fun saveContextEmoji(
            emojiKey: Preferences.Key<String>,
            emoji: String,
        ) {
            context.dataStore.edit { settings -> settings[emojiKey] = emoji }
        }

        val customContextNamesFlow: Flow<Set<String>> =
            context.dataStore.data
                .map { preferences ->
                    try {
                        preferences[ContextKeys.CUSTOM_CONTEXT_NAMES] ?: emptySet()
                    } catch (e: ClassCastException) {
                        val corruptedValue = preferences[stringPreferencesKey(ContextKeys.CUSTOM_CONTEXT_NAMES.name)]
                        if (corruptedValue.isNullOrBlank() || corruptedValue == "[]" || !corruptedValue.startsWith("[")) {
                            emptySet()
                        } else {
                            corruptedValue
                                .substring(1, corruptedValue.length - 1)
                                .split(", ")
                                .filter { it.isNotBlank() }
                                .toSet()
                        }
                    }
                }

        suspend fun saveCustomContextNames(names: Set<String>) {
            context.dataStore.edit { settings -> settings[ContextKeys.CUSTOM_CONTEXT_NAMES] = names }
        }

        private fun customContextTagKey(name: String) = stringPreferencesKey("custom_context_tag_${name.lowercase()}")

        private fun customContextEmojiKey(name: String) = stringPreferencesKey("custom_context_emoji_${name.lowercase()}")

        fun getCustomContextTagFlow(name: String): Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[customContextTagKey(name)] ?: "" }

        suspend fun saveCustomContextTag(
            name: String,
            tag: String,
        ) {
            context.dataStore.edit { settings -> settings[customContextTagKey(name)] = tag }
        }

        fun getCustomContextEmojiFlow(name: String): Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[customContextEmojiKey(name)] ?: "" }

        suspend fun saveCustomContextEmoji(
            name: String,
            emoji: String,
        ) {
            context.dataStore.edit { settings -> settings[customContextEmojiKey(name)] = emoji }
        }

        suspend fun deleteCustomContext(name: String) {
            val currentNames = customContextNamesFlow.first().toMutableSet()
            if (currentNames.remove(name)) {
                saveCustomContextNames(currentNames)
            }
            context.dataStore.edit { settings ->
                settings.remove(customContextTagKey(name))
                settings.remove(customContextEmojiKey(name))
            }
        }

        suspend fun getPreferencesSnapshot(): Preferences = context.dataStore.data.first()

        suspend fun restoreFromMap(settingsMap: Map<String, String>) {
            context.dataStore.edit { preferences ->
                preferences.clear()
                for ((key, value) in settingsMap) {
                    when (key) {
                        showPlanningModesKey.name -> {
                            preferences[booleanPreferencesKey(key)] = value.toBoolean()
                        }

                        TEMPERATURE_KEY.name -> {
                            preferences[floatPreferencesKey(key)] = value.toFloatOrNull() ?: 0.8f
                        }

                        ContextKeys.CUSTOM_CONTEXT_NAMES.name -> {
                            val restoredSet: Set<String>
                            if (value == "[]" || !value.startsWith("[") || !value.endsWith("]")) {
                                restoredSet = emptySet()
                            } else {
                                restoredSet =
                                    value
                                        .substring(1, value.length - 1)
                                        .split(", ")
                                        .filter { it.isNotBlank() }
                                        .toSet()
                            }
                            preferences[stringSetPreferencesKey(key)] = restoredSet
                        }

                        else -> {
                            preferences[stringPreferencesKey(key)] = value
                        }
                    }
                }
            }
        }

        val rolesFolderUriFlow: Flow<String> =
            context.dataStore.data
                .map { preferences -> preferences[ROLES_FOLDER_URI_KEY] ?: "" }

        suspend fun saveRolesFolderUri(uri: String) {
            context.dataStore.edit { settings -> settings[ROLES_FOLDER_URI_KEY] = uri }
        }
    }
