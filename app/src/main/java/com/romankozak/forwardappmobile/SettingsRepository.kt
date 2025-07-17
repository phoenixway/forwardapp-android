package com.romankozak.forwardappmobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val desktopAddressKey = stringPreferencesKey("desktop_address")
    // --- ДОДАНО КЛЮЧ ДЛЯ VAULT ---
    private val obsidianVaultNameKey = stringPreferencesKey("obsidian_vault_name")

    val desktopAddressFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[desktopAddressKey] ?: ""
        }

    suspend fun saveDesktopAddress(address: String) {
        context.dataStore.edit { settings ->
            settings[desktopAddressKey] = address
        }
    }

    // --- ДОДАНО МЕТОДИ ДЛЯ VAULT ---
    val obsidianVaultNameFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[obsidianVaultNameKey] ?: "" // Повертаємо пустий рядок, якщо не задано
        }

    suspend fun saveObsidianVaultName(name: String) {
        context.dataStore.edit { settings ->
            settings[obsidianVaultNameKey] = name
        }
    }
}