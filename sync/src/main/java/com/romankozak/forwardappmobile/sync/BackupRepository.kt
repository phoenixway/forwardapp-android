package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.sync.FullAppBackup
import com.romankozak.forwardappmobile.sync.SyncLocalService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for creating and restoring full application backups.
 * Handles file operations and tracks sync timestamps.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val syncFileService: SyncFileService,
    private val syncLocalService: SyncLocalService,
) {
    /**
     * Exports full backup to a file in Downloads folder
     * @return Result with file path or error
     */
    suspend fun exportFullBackupToFile(): Result<String> =
        syncFileService.exportFullBackupToFile()

    /**
     * Creates JSON string with complete backup of all data
     * @return JSON string with full backup
     */
    suspend fun createFullBackupJsonString(): String =
        syncFileService.createFullBackupJsonString()

    /**
     * Imports full backup from a file
     * @param uri URI of the backup file
     * @return Result with success message or error
     */
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> =
        syncFileService.importFullBackupFromFile(uri)

    /**
     * Parses backup file without importing
     * @param uri URI of the backup file
     * @return Result with parsed FullAppBackup or error
     */
    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> =
        syncFileService.parseBackupFile(uri)

    /**
     * Gets the timestamp of the last successful sync
     * Finds the minimum syncedAt timestamp across all entities
     * @return Last sync timestamp or null if never synced
     */
    suspend fun getLastSyncTime(): Long? {
        val local = syncLocalService.loadLocalDatabaseContent()
        val allSyncedTimes = listOfNotNull(
            local.projects.mapNotNull { it.syncedAt }.minOrNull(),
            local.goals.mapNotNull { it.syncedAt }.minOrNull(),
            local.documents.mapNotNull { it.syncedAt }.minOrNull(),
            local.attachments.mapNotNull { it.syncedAt }.minOrNull(),
            local.contextAttachmentCrossRefs.mapNotNull { it.syncedAt }.minOrNull(),
            local.backlogOrders.mapNotNull { it.syncedAt }.minOrNull(),
        )
        return allSyncedTimes.minOrNull()
    }
}