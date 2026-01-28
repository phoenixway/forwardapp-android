package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange
import com.romankozak.forwardappmobile.core.data.models.sync.SyncReport
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main synchronization repository that coordinates sync operations
 * by delegating to specialized repositories.
 *
 * This repository acts as a facade, providing a unified interface
 * for all sync-related operations while maintaining separation of concerns.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val backupRepository: BackupRepository,
    private val wifiSyncRepository: WifiSyncRepository,
    private val mergeRepository: MergeRepository,
    private val attachmentsRepository: AttachmentsRepository,
) : SyncApi {
    // ============================================
    // FILE BACKUP/RESTORE OPERATIONS
    // ============================================

    /**
     * Exports full backup to Downloads folder
     */
    override suspend fun exportFullBackupToFile(): Result<String> =
        backupRepository.exportFullBackupToFile()

    /**
     * Creates JSON string with complete backup
     */
    override suspend fun createFullBackupJsonString(): String =
        backupRepository.createFullBackupJsonString()

    /**
     * Imports full backup from file URI
     */
    override suspend fun importFullBackupFromFile(uri: Uri): Result<String> =
        backupRepository.importFullBackupFromFile(uri)

    /**
     * Parses backup file without importing
     */
    override suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> =
        backupRepository.parseBackupFile(uri)

    // ============================================
    // WI-FI SYNC OPERATIONS
    // ============================================

    /**
     * Fetches backup from Wi-Fi server
     */
    override suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> =
        wifiSyncRepository.fetchBackupFromWifi(address, deltaSince)

    /**
     * Pushes unsynced changes to Wi-Fi server
     */
    override suspend fun pushUnsyncedToWifi(address: String): Result<Unit> =
        wifiSyncRepository.pushUnsyncedToWifi(address)

    /**
     * Creates delta backup JSON since timestamp
     */
    override suspend fun createDeltaBackupJsonString(deltaSince: Long): String =
        wifiSyncRepository.createDeltaBackupJsonString(deltaSince)

    // ============================================
    // SYNC OPERATIONS
    // ============================================

    /**
     * Gets timestamp of last successful sync
     */
    override suspend fun getLastSyncTime(): Long? =
        backupRepository.getLastSyncTime()

    /**
     * Creates sync report by comparing backup with local data
     */
    override suspend fun createSyncReport(jsonString: String): SyncReport =
        mergeRepository.createSyncReport(jsonString)

    /**
     * Applies approved changes to database
     */
    override suspend fun applyChanges(approvedChanges: List<SyncChange>) =
        mergeRepository.applyChanges(approvedChanges)

    /**
     * Applies server changes with merging and conflict resolution
     */
    override suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> =
        mergeRepository.applyServerChanges(changes)

    /**
     * Creates diff between incoming and local data
     */
    override suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff =
        mergeRepository.createBackupDiff(incoming)

    /**
     * Imports only selected data from backup
     */
    override suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> =
        mergeRepository.importSelectedData(selectedData)

    // ============================================
    // ATTACHMENTS BACKUP/RESTORE
    // ============================================

    /**
     * Exports attachments to Downloads folder
     */
    override suspend fun exportAttachmentsToFile(): Result<String> =
        attachmentsRepository.exportAttachmentsToFile()

    /**
     * Creates JSON string with all attachments
     */
    override suspend fun createAttachmentsBackupJsonString(): String =
        attachmentsRepository.createAttachmentsBackupJsonString()

    /**
     * Imports attachments from file URI
     */
    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> =
        attachmentsRepository.importAttachmentsFromFile(uri)
}