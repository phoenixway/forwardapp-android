package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val desktopAddressKey = stringPreferencesKey("desktop_address")
    private val obsidianVaultNameKey = stringPreferencesKey("obsidian_vault_name")
    private val showPlanningModesKey = booleanPreferencesKey("show_planning_modes")
    private val dailyTagKey = stringPreferencesKey("daily_planning_tag")
    private val mediumTagKey = stringPreferencesKey("medium_planning_tag")
    private val longTagKey = stringPreferencesKey("long_planning_tag")

    // ✨ --- ЗМІНЕНО: Ключі для тегів КОНТЕКСТІВ --- ✨
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


    }

    // ... (код для desktopAddress, obsidianVaultName, planning modes) ...
    val desktopAddressFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[desktopAddressKey] ?: "" }

    suspend fun saveDesktopAddress(address: String) {
        context.dataStore.edit { settings -> settings[desktopAddressKey] = address }
    }

    val obsidianVaultNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[obsidianVaultNameKey] ?: "" }

    suspend fun saveObsidianVaultName(name: String) {
        context.dataStore.edit { settings -> settings[obsidianVaultNameKey] = name }
    }

    suspend fun getObsidianVaultName(): String {
        return obsidianVaultNameFlow.first()
    }

    val showPlanningModesFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[showPlanningModesKey] ?: false }

    suspend fun saveShowPlanningModes(show: Boolean) {
        context.dataStore.edit { settings -> settings[showPlanningModesKey] = show }
    }

    val dailyTagFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[dailyTagKey] ?: "daily" }

    suspend fun saveDailyTag(tag: String) {
        context.dataStore.edit { settings -> settings[dailyTagKey] = tag }
    }

    val mediumTagFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[mediumTagKey] ?: "medium" }

    suspend fun saveMediumTag(tag: String) {
        context.dataStore.edit { settings -> settings[mediumTagKey] = tag }
    }

    val longTagFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[longTagKey] ?: "long" }

    suspend fun saveLongTag(tag: String) {
        context.dataStore.edit { settings -> settings[longTagKey] = tag }
    }

// Знайдіть цю функцію і замініть її повністю

    fun getContextTagFlow(contextKey: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.map { preferences ->
            val contextName = contextKey.name.removePrefix("context_tag_")
            val savedTag = preferences[contextKey]

            // ✨ ВИПРАВЛЕННЯ: Повертаємо значення за замовчуванням,
            // якщо збережений тег є null АБО порожнім/пробільним.
            if (savedTag.isNullOrBlank()) {
                "${contextName}_context_tag"
            } else {
                savedTag
            }
        }
    }

    suspend fun saveContextTag(contextKey: Preferences.Key<String>, tag: String) {
        context.dataStore.edit { settings -> settings[contextKey] = tag }
    }

    fun getContextEmojiFlow(emojiKey: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[emojiKey] ?: "" // Повертаємо порожній рядок, якщо емодзі не встановлено
        }
    }

    suspend fun saveContextEmoji(emojiKey: Preferences.Key<String>, emoji: String) {
        context.dataStore.edit { settings -> settings[emojiKey] = emoji }
    }

}