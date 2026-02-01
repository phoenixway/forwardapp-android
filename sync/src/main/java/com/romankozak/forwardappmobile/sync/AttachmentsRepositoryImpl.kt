package com.romankozak.forwardappmobile.sync

import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AttachmentsRepositoryImpl @Inject constructor(
    private val localDataSource: AttachmentsLocalDataSource,
    private val contentProvider: IContentProvider, // Впроваджуємо новий провайдер
    private val logicHelper: SyncLogicHelper,
) : AttachmentsRepository {

    private val tag = "AttachmentsRepository"
    private val gson = GsonBuilder().create()

    override suspend fun exportAttachmentsToFile(): Result<String> =
        try {
            val backupJson = createAttachmentsBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_attachments_$timestamp.json"

            // Використовуємо contentProvider замість syncFileService
            val saveResult = contentProvider.saveFile(fileName, backupJson)

            if (saveResult.isSuccess) {
                Result.success("Вкладення успішно збережено")
            } else {
                Result.failure(saveResult.exceptionOrNull() ?: Exception("Unknown save error"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error exporting attachments", e)
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

    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> =
        try {
            // Читаємо текст через contentProvider, конвертуючи Uri в String
            val jsonResult = contentProvider.readText(uri.toString())
            val jsonString = jsonResult.getOrThrow()

            if (jsonString.isBlank()) throw Exception("File is empty")

            val backupData = gson.fromJson(jsonString, AttachmentsBackup::class.java)

            val orphanCount = localDataSource.importAttachments(backupData)
            Result.success("Імпорт завершено. Знайдено $orphanCount вкладень без прив'язки.")
        } catch (e: Exception) {
            Log.e(tag, "Import error", e)
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

    override suspend fun findAttachmentByEntity(
        attachmentType: String,
        entityId: String,
    ): AttachmentEntity? = localDataSource.findAttachmentByEntity(attachmentType, entityId)

    override suspend fun deleteAttachment(attachmentId: String) {
        localDataSource.deleteAttachment(attachmentId)
    }

    // ВИПРАВЛЕНО: Тип повернення змінено на Flow<List<AttachmentLibraryQueryResult>>
    override fun getAttachmentLibraryItems(): Flow<List<AttachmentLibraryQueryResult>> =
        localDataSource.getAttachmentLibraryItems()

    // ВИПРАВЛЕНО: Тип повернення змінено на Flow<List<ContextAttachmentCrossRef>>
    override fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>> =
        localDataSource.getAllAttachmentLinks()

    override suspend fun linkAttachmentToContext(
        attachmentId: String,
        contextId: String,
    ): Unit = localDataSource.linkAttachmentToContext(attachmentId, contextId)

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