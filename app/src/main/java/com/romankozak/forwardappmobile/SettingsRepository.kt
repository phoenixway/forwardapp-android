package com.romankozak.forwardappmobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Створюємо DataStore за допомогою делегата
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // Створюємо ключ для збереження адреси
    private val desktopAddressKey = stringPreferencesKey("desktop_address")

    // Flow для читання адреси. Компоненти зможуть підписатися на нього.
    val desktopAddressFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[desktopAddressKey] ?: "" // Повертаємо пустий рядок, якщо нічого не збережено
        }

    // Функція для збереження адреси
    suspend fun saveDesktopAddress(address: String) {
        context.dataStore.edit { settings ->
            settings[desktopAddressKey] = address
        }
    }
}