package com.romankozak.forwardappmobile.sync

import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AttachmentsRepositoryImpl
@Inject
constructor(
    private val localDataSource: AttachmentsLocalDataSource,
    private val syncFileService: SyncFileService,
    private val logicHelper: SyncLogicHelper,
) : AttachmentsRepository {

  private val TAG = "AttachmentsRepository"
  private val gson = GsonBuilder().create()

  override suspend fun exportAttachmentsToFile(): Result<String> =
      try {
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
    val synthesizedCrossRefs =
        logicHelper.synthesizeMissingCrossRefs(
            attachments = backup.attachments,
            existingCrossRefs = backup.contextAttachmentCrossRefs,
        )

    val finalBackup = backup.copy(contextAttachmentCrossRefs = synthesizedCrossRefs)
    return gson.toJson(finalBackup)
  }

  override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> =
      try {
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
      isSystem: Boolean,
  ) {
    localDataSource.ensureAttachmentLinkedToContext(
        attachmentType,
        entityId,
        contextId,
        ownerContextId,
        createdAt,
        roleCode,
        isSystem,
    )
  }

  override suspend fun findAttachmentByEntity(attachmentType: String, entityId: String) =
      localDataSource.findAttachmentByEntity(attachmentType, entityId)

  override suspend fun deleteAttachment(attachmentId: String) {
    localDataSource.deleteAttachment(attachmentId)
  }

  override fun getAttachmentLibraryItems() = localDataSource.getAttachmentLibraryItems()

  override fun getAllAttachmentLinks() = localDataSource.getAllAttachmentLinks()

  override suspend fun linkAttachmentToContext(attachmentId: String, contextId: String) =
      localDataSource.linkAttachmentToContext(attachmentId, contextId)

  override fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>> =
      localDataSource.getAttachmentsForContext(contextId)

  override suspend fun getAttachmentById(id: String): AttachmentEntity? =
      localDataSource.getAttachmentById(id)

  override suspend fun unlinkAttachmentFromContext(attachmentId: String, contextId: String) {
    localDataSource.unlinkAttachmentFromContext(attachmentId, contextId)
  }

  override suspend fun updateAttachmentOrders(contextId: String, orders: Map<String, Long>) {
    localDataSource.updateAttachmentOrders(contextId, orders)
  }

  // File: sync/.../AttachmentsRepositoryImpl.kt

  override suspend fun createLinkAttachment(
      contextId: String,
      link: RelatedLink,
      roleCode: String?,
      isSystem: Boolean,
  ): String {
    return localDataSource.createLinkAttachment(
        contextId = contextId,
        link = link,
        roleCode = roleCode,
        isSystem = isSystem,
    )
  }

  override suspend fun findAttachmentByRole(
      contextId: String,
      roleCode: String,
  ): AttachmentEntity? {
    return localDataSource.findAttachmentByRole(contextId, roleCode)
  }
}
