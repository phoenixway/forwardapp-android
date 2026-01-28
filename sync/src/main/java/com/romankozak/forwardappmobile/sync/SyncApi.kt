package com.romankozak.forwardappmobile.sync

import android.net.Uri

interface SyncApi {
    suspend fun exportFullBackupToFile(): Result<String>
    suspend fun createFullBackupJsonString(): String
    suspend fun importFullBackupFromFile(uri: Uri): Result<String>
    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup>

    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String>
    suspend fun pushUnsyncedToWifi(address: String): Result<Unit>
    suspend fun createDeltaBackupJsonString(deltaSince: Long): String

    suspend fun getLastSyncTime(): Long?
    suspend fun createSyncReport(jsonString: String): SyncReport
    suspend fun applyChanges(approvedChanges: List<SyncChange>)
    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit>
    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff
    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String>

    suspend fun exportAttachmentsToFile(): Result<String>
    suspend fun createAttachmentsBackupJsonString(): String
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String>
}
