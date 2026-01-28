package com.romankozak.forwardappmobile.sync

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWifiService @Inject constructor() {
    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun createDeltaBackupJsonString(deltaSince: Long): String = "Sync disabled"
}
