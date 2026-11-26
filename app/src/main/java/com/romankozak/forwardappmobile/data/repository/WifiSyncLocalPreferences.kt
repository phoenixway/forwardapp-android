package com.romankozak.forwardappmobile.data.repository

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal val WIFI_SYNC_DEVICE_LOCAL_KEYS = setOf(
    "wifi_sync_server_key",
    "wifi_sync_peer_key",
    "desktop_sync_address",
    "wifi_sync_port",
    "wifi_sync_server_enabled",
)

/** Full JSON Restore replaces ordinary preferences but keeps this device's connection identity. */
internal fun MutablePreferences.clearKeepingWifiSyncLocal() {
    val local = asMap().filterKeys { it.name in WIFI_SYNC_DEVICE_LOCAL_KEYS }
    clear()
    local.forEach { (key, value) ->
        when (value) {
            is Boolean -> this[booleanPreferencesKey(key.name)] = value
            is Int -> this[intPreferencesKey(key.name)] = value
            is String -> this[stringPreferencesKey(key.name)] = value
        }
    }
}
