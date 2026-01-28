package com.romankozak.forwardappmobile.sync

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor() {
    suspend fun exportFullBackupToFile(): Result<String> = Result.success("Sync disabled")
    suspend fun createFullBackupJsonString(): String = "Sync disabled"
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun getLastSyncTime(): Long? = null
}
