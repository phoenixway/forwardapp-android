package com.romankozak.forwardappmobile.features.sync

sealed interface WifiAutoSyncStatus {
    data object Off : WifiAutoSyncStatus
    data object Paused : WifiAutoSyncStatus
    data object NeedsSetup : WifiAutoSyncStatus
    data object NeedsServer : WifiAutoSyncStatus
    data class Syncing(val peerName: String, val peerCount: Int) : WifiAutoSyncStatus
    data class Result(val successful: Int, val total: Int, val at: Long?, val error: String?) : WifiAutoSyncStatus
}
