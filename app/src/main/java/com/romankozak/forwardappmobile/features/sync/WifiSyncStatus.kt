package com.romankozak.forwardappmobile.features.sync

sealed class WifiSyncStatus {
    object Disabled : WifiSyncStatus()
    object Idle : WifiSyncStatus()
    object Syncing : WifiSyncStatus()
    data class ServerRunning(val address: String?) : WifiSyncStatus()
    data class Error(val message: String) : WifiSyncStatus()
    object Offline : WifiSyncStatus()
}
