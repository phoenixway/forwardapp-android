// core-data-interfaces/.../datasource/SyncSettingsSource.kt
package com.romankozak.forwardappmobile.sync.datasource

import kotlinx.coroutines.flow.Flow

interface SyncSettingsSource {
    val wifiSyncPortFlow: Flow<Int>
}