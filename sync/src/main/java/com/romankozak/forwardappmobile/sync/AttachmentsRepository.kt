package com.romankozak.forwardappmobile.sync

import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup
import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for exporting and importing attachments
 * (documents, checklists, links) with their cross-references.
 * This repository is now decoupled from the database implementation.
 */
@Singleton
class AttachmentsRepository @Inject constructor(
    private val localDataSource: AttachmentsLocalDataSource,
    private val syncFileService: SyncFileService,
    private val logicHelper: SyncLogicHelper,
) {
    private val TAG = "AttachmentsRepository"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    /**
     * Exports all attachments to a JSON file in Downloads folder
     * @return Result with success message or error
     */
    suspend fun exportAttachmentsToFile(): Result<String> = try {
        val backupJson = createAttachmentsBackupJsonString()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "forward_attachments_$timestamp.json"
        syncFileService.saveFileToDownloads(fileName, backupJson)
        Result.success("Вкладення успішно збережено до Downloads/ForwardApp")
    } catch (e: Exception) {
        Log.e(TAG, "Error exporting attachments", e)
        Result.failure(e)
    }

    /**
     * Creates JSON string with all attachments data
     * Includes documents, checklists, links, and cross-references
     * @return JSON string with attachments backup
     */
    suspend fun createAttachmentsBackupJsonString(): String {
        Log.d(TAG, "=== СТАРТ ЕКСПОРТУ ВКЛАДЕНЬ ===")
        val backup = localDataSource.getAttachmentsBackup()

        // Synthesize missing cross-references if needed
        val synthesizedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = backup.attachments,
            existingCrossRefs = backup.contextAttachmentCrossRefs,
        )

        val finalBackup = backup.copy(contextAttachmentCrossRefs = synthesizedCrossRefs)

        return gson.toJson(finalBackup)
    }

    /**
     * Imports attachments from JSON file
     * Validates context IDs and filters orphaned attachments via the data source.
     * @param uri URI of the backup file
     * @return Result with import summary or error
     */
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String> {
        val IMPORT_TAG = "AttachmentsRepo_Import"
        try {
            val jsonString = syncFileService.readTextFromUri(uri) ?: throw Exception("File is empty")
            val backupData = gson.fromJson(jsonString, AttachmentsBackup::class.java)

            val orphanCount = localDataSource.importAttachments(backupData)

            return Result.success("Імпорт завершено. Знайдено $orphanCount вкладень без прив'язки до проектів.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Critical error during attachments import", e)
            return Result.failure(e)
        }
    }
}