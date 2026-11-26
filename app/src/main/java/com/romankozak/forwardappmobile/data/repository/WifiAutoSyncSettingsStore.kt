package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wifiAutoSyncDataStore by preferencesDataStore(name = "wifi_auto_sync_local")
private val enabledKey = booleanPreferencesKey("enabled")
private val primaryEnabledKey = booleanPreferencesKey("primary_enabled")
private val primaryNameKey = stringPreferencesKey("primary_name")
private val extraPeersKey = stringPreferencesKey("extra_peers")

/** Local connection details, never part of a SnapshotBundle or JSON backup. */
data class WifiAutoSyncPeer(
    val id: String,
    val name: String,
    val address: String,
    val key: String,
    val enabled: Boolean = true,
)

@Singleton
class WifiAutoSyncSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val gson = Gson()
    private val peerListType = object : TypeToken<List<WifiAutoSyncPeer>>() {}.type

    val enabledFlow: Flow<Boolean> = context.wifiAutoSyncDataStore.data.map { it[enabledKey] ?: false }
    val primaryEnabledFlow: Flow<Boolean> = context.wifiAutoSyncDataStore.data.map { it[primaryEnabledKey] ?: true }
    val primaryNameFlow: Flow<String> = context.wifiAutoSyncDataStore.data.map { it[primaryNameKey] ?: "Основний пристрій" }

    val extraPeersFlow: Flow<List<WifiAutoSyncPeer>> = context.wifiAutoSyncDataStore.data.map { prefs ->
        val raw = prefs[extraPeersKey].orEmpty()
        if (raw.isBlank()) emptyList() else runCatching {
            (gson.fromJson<List<WifiAutoSyncPeer>>(raw, peerListType) ?: emptyList())
                .filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    val peersFlow: Flow<List<WifiAutoSyncPeer>> = combine(
        settingsRepository.wifiSyncPrimaryPeerFlow,
        extraPeersFlow,
        primaryEnabledFlow,
        primaryNameFlow,
    ) { primaryConnection, extras, primaryEnabled, primaryName ->
        val (primaryAddress, primaryKey) = primaryConnection
        val primary = if (primaryAddress.isNotBlank() && primaryKey.isNotBlank()) {
            listOf(WifiAutoSyncPeer("primary", primaryName, primaryAddress.trim(), primaryKey.trim(), primaryEnabled))
        } else emptyList()
        (primary + extras).filter { it.enabled && it.address.isNotBlank() && it.key.isNotBlank() }
            .distinctBy { it.address.trim().lowercase() }
    }

    suspend fun save(enabled: Boolean, extraPeers: List<WifiAutoSyncPeer>) {
        context.wifiAutoSyncDataStore.edit { prefs ->
            prefs[enabledKey] = enabled
            prefs[extraPeersKey] = gson.toJson(extraPeers.filter { it.id != "primary" })
        }
    }

    suspend fun disable() {
        context.wifiAutoSyncDataStore.edit { it[enabledKey] = false }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.wifiAutoSyncDataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setPrimaryEnabled(enabled: Boolean) {
        context.wifiAutoSyncDataStore.edit { it[primaryEnabledKey] = enabled }
    }

    suspend fun setPrimaryName(name: String) {
        context.wifiAutoSyncDataStore.edit { it[primaryNameKey] = name.ifBlank { "Основний пристрій" } }
    }

    suspend fun upsertExtraPeer(peer: WifiAutoSyncPeer) {
        require(peer.id != "primary")
        context.wifiAutoSyncDataStore.edit { prefs ->
            val current = decodePeers(prefs[extraPeersKey].orEmpty())
            prefs[extraPeersKey] = gson.toJson(current.filterNot { it.id == peer.id } + peer)
        }
    }

    suspend fun removeExtraPeer(id: String) {
        context.wifiAutoSyncDataStore.edit { prefs ->
            prefs[extraPeersKey] = gson.toJson(decodePeers(prefs[extraPeersKey].orEmpty()).filterNot { it.id == id })
        }
    }

    suspend fun setExtraPeerEnabled(id: String, enabled: Boolean) {
        context.wifiAutoSyncDataStore.edit { prefs ->
            prefs[extraPeersKey] = gson.toJson(decodePeers(prefs[extraPeersKey].orEmpty()).map { peer ->
                if (peer.id == id) peer.copy(enabled = enabled) else peer
            })
        }
    }

    private fun decodePeers(raw: String): List<WifiAutoSyncPeer> =
        if (raw.isBlank()) emptyList() else runCatching {
            gson.fromJson<List<WifiAutoSyncPeer>>(raw, peerListType).orEmpty().filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())
}
