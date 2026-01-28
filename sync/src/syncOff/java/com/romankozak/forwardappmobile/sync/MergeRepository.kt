package com.romankozak.forwardappmobile.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeRepository @Inject constructor() {
    suspend fun createSyncReport(jsonString: String): SyncReport = SyncReport(emptyList())
    suspend fun applyChanges(approvedChanges: List<SyncChange>) { /* no-op */ }
    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff = BackupDiff()
    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
}
