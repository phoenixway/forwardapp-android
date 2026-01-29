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

@Singleton
class AttachmentsRepositoryImpl @Inject constructor(
    private val localDataSource: AttachmentsLocalDataSource,
    private val syncFileService: SyncFileService,
    private val logicHelper: SyncLogicHelper,
) : AttachmentsRepository {

    private val TAG = "AttachmentsRepository"
    private val gson = GsonBuilder().create()

    override suspend fun exportAttachmentsToFile(): Result<String> = try {
        val backupJson = createAttachmentsBackupJsonString()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "forward_attachments_$timestamp.json"

        // Викликаємо метод збереження у файл
        syncFileService.saveFileToDownloads(fileName, backupJson)

        Result.success("Вкладення успішно збережено")
    } catch (e: Exception) {
        Log.e(TAG, "Error exporting attachments", e)
        Result.failure(e)
    }

    override suspend fun createAttachmentsBackupJsonString(): String {
        val backup = localDataSource.getAttachmentsBackup()

        // Викликаємо помічник для логіки крос-посилань
        val synthesizedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = backup.attachments,
            existingCrossRefs = backup.contextAttachmentCrossRefs,
        )

        val finalBackup = backup.copy(contextAttachmentCrossRefs = synthesizedCrossRefs)
        return gson.toJson(finalBackup)
    }

    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> = try {
        // Читаємо JSON через сервіс файлів
        val jsonString = syncFileService.readTextFromUri(uri) ?: throw Exception("File is empty")
        val backupData = gson.fromJson(jsonString, AttachmentsBackup::class.java)

        val orphanCount = localDataSource.importAttachments(backupData)
        Result.success("Імпорт завершено. Знайдено $orphanCount вкладень без прив'язки.")
    } catch (e: Exception) {
        Log.e(TAG, "Import error", e)
        Result.failure(e)
    }

    override suspend fun ensureAttachmentLinkedToContext(
        attachmentType: String,
        entityId: String,
        contextId: String,
        ownerContextId: String?,
        createdAt: Long,
        roleCode: String?,
        isSystem: Boolean
    ) {
        localDataSource.ensureAttachmentLinkedToContext(
            attachmentType, entityId, contextId, ownerContextId, createdAt, roleCode, isSystem
        )
    }

    override suspend fun findAttachmentByEntity(attachmentType: String, entityId: String) =
        localDataSource.findAttachmentByEntity(attachmentType, entityId)

    override suspend fun deleteAttachment(attachmentId: String) {
        localDataSource.deleteAttachment(attachmentId)
    }
}