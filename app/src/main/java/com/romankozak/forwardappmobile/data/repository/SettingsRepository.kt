package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey // <-- Додайте імпорт
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

    // ✨ --- ДОДАНО НОВІ КЛЮЧІ ДЛЯ РЕЖИМІВ ПЛАНУВАННЯ --- ✨
    private val showPlanningModesKey = booleanPreferencesKey("show_planning_modes")
    private val dailyTagKey = stringPreferencesKey("daily_planning_tag")
    private val mediumTagKey = stringPreferencesKey("medium_planning_tag")
    private val longTagKey = stringPreferencesKey("long_planning_tag")
    // --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ ---

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

    // ✨ --- ДОДАНО НОВІ FLOW ТА ФУНКЦІЇ ЗБЕРЕЖЕННЯ --- ✨

    val showPlanningModesFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[showPlanningModesKey] ?: false }

    suspend fun saveShowPlanningModes(show: Boolean) {
        context.dataStore.edit { settings -> settings[showPlanningModesKey] = show }
    }

    val dailyTagFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[dailyTagKey] ?: "daily" } // Значення за замовчуванням

    suspend fun saveDailyTag(tag: String) {
        context.dataStore.edit { settings -> settings[dailyTagKey] = tag }
    }

    val mediumTagFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[mediumTagKey] ?: "medium" } // Значення за замовчуванням

    suspend fun saveMediumTag(tag: String) {
        context.dataStore.edit { settings -> settings[mediumTagKey] = tag }
    }

    val longTagFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[longTagKey] ?: "long" } // Значення за замовчуванням

    suspend fun saveLongTag(tag: String) {
        context.dataStore.edit { settings -> settings[longTagKey] = tag }
    }
    // --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ --- ✨ ---
}