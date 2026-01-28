package com.romankozak.forwardappmobile.sync

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSyncRepository @Inject constructor() : SyncApi {
    override suspend fun exportFullBackupToFile(): Result<String> = Result.success("Sync is disabled in this build.")
    override suspend fun createFullBackupJsonString(): String = "Sync is disabled in this build."
    override suspend fun importFullBackupFromFile(uri: Uri): Result<String> = Result.success("Sync is disabled in this build.")
    override suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = Result.failure(UnsupportedOperationException("Sync is disabled in this build."))

    override suspend fun fetchBackupFromWifi(address: String, deltaSince: Long?): Result<String> = Result.failure(UnsupportedOperationException("Sync is disabled in this build."))
    override suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = Result.failure(UnsupportedOperationException("Sync is disabled in this build."))
    override suspend fun createDeltaBackupJsonString(deltaSince: Long): String = "Sync is disabled in this build."

    override suspend fun getLastSyncTime(): Long? = null
    override suspend fun createSyncReport(jsonString: String): SyncReport = SyncReport(emptyList())
    override suspend fun applyChanges(approvedChanges: List<SyncChange>) { /* no-op */ }
    override suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> = Result.failure(UnsupportedOperationException("Sync is disabled in this build."))
    override suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff = BackupDiff()
    override suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> = Result.failure(UnsupportedOperationException("Sync is disabled in this build."))

    override suspend fun exportAttachmentsToFile(): Result<String> = Result.success("Sync is disabled in this build.")
    override suspend fun createAttachmentsBackupJsonString(): String = "Sync is disabled in this build."
    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> = Result.failure(UnsupportedOperationException("Sync is disabled in this build."))
}
