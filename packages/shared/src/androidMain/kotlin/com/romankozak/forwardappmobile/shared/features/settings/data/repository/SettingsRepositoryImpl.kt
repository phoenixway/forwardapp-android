package com.romankozak.forwardappmobile.shared.features.settings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.romankozak.forwardappmobile.shared.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val CUSTOM_CONTEXT_NAMES = stringSetPreferencesKey("custom_context_names")

    override val customContextNamesFlow: Flow<Set<String>> =
        context.dataStore.data.map { preferences ->
            preferences[CUSTOM_CONTEXT_NAMES] ?: emptySet()
        }

    override fun getContextTagFlow(contextKey: String): Flow<String> =
        context.dataStore.data.map { preferences ->
            val key = stringPreferencesKey(contextKey)
            val contextName = key.name.removePrefix("context_tag_")
            val savedTag = preferences[key]
            if (savedTag.isNullOrBlank()) {
                "${contextName}_context_tag"
            } else {
                savedTag
            }
        }

    override fun getContextEmojiFlow(emojiKey: String): Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(emojiKey)] ?: ""
        }

    override fun getCustomContextTagFlow(name: String): Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("custom_context_tag_${name.lowercase()}")] ?: ""
        }

    override fun getCustomContextEmojiFlow(name: String): Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("custom_context_emoji_${name.lowercase()}")] ?: ""
        }
}
