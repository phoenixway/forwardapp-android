package com.romankozak.forwardappmobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

// Кажемо Hilt, що цей клас має бути єдиним на весь додаток (Singleton)
@Singleton
// Кажемо Hilt, як створювати цей клас (за допомогою ін'єкції в конструктор)
class SettingsRepository @Inject constructor(
    // Кажемо Hilt, що сюди треба "вставити" контекст рівня додатку
    @ApplicationContext private val context: Context
) {

    private val desktopAddressKey = stringPreferencesKey("desktop_address")
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

    val obsidianVaultNameFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[obsidianVaultNameKey] ?: ""
        }

    suspend fun saveObsidianVaultName(name: String) {
        context.dataStore.edit { settings ->
            settings[obsidianVaultNameKey] = name
        }
    }

    // --- ДОДАНО SUSPEND-ФУНКЦІЮ ДЛЯ VIEWMODEL ---
    // Ця функція одноразово отримує значення з Flow.
    // Вона потрібна для ініціалізації у ViewModel.
    suspend fun getObsidianVaultName(): String {
        return obsidianVaultNameFlow.first()
    }
}