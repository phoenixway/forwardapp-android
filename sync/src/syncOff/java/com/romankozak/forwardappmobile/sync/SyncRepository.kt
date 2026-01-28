package com.romankozak.forwardappmobile.sync

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor() {
    suspend fun exportFullBackupToFile(): Result<String> = Result.success("Sync disabled")
    suspend fun createFullBackupJsonString(): String = "Sync disabled"
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> = Result.success("Sync disabled")
    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = Result.failure(UnsupportedOperationException("Sync disabled"))

    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun createDeltaBackupJsonString(deltaSince: Long): String = "Sync disabled"

    suspend fun getLastSyncTime(): Long? = null
    suspend fun createSyncReport(jsonString: String): SyncReport = SyncReport(emptyList())
    suspend fun applyChanges(approvedChanges: List<SyncChange>) { /* no-op */ }
    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff = BackupDiff()
    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))

    suspend fun exportAttachmentsToFile(): Result<String> = Result.success("Sync disabled")
    suspend fun createAttachmentsBackupJsonString(): String = "Sync disabled"
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
}
