package com.romankozak.forwardappmobile.sync

import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for exporting and importing attachments
 * (documents, checklists, links) with their cross-references.
 */
@Singleton
class AttachmentsRepository @Inject constructor(
    private val appDatabase: com.romankozak.forwardappmobile.database.AppDatabase,
    private val syncFileService: SyncFileService,
    private val logicHelper: SyncLogicHelper,
    private val contextDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao,
    private val noteDocumentDao: com.romankozak.forwardappmobile.features.attachments.data.NoteDocumentDao,
    private val checklistDao: com.romankozak.forwardappmobile.features.attachments.data.ChecklistDao,
    private val linkItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao,
    private val attachmentDao: com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao,
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
        val attachments = attachmentDao.getAll()
        val crossRefs = attachmentDao.getAllContextAttachmentCrossRefs()

        val synthesizedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = attachments,
            existingCrossRefs = crossRefs,
        )

        val attachmentsBackup = AttachmentsBackup(
            documents = noteDocumentDao.getAllDocuments(),
            documentItems = noteDocumentDao.getAllDocumentItems(),
            checklists = checklistDao.getAllChecklists(),
            checklistItems = checklistDao.getAllChecklistItems(),
            linkItemEntities = linkItemDao.getAllEntities(),
            attachments = attachments,
            contextAttachmentCrossRefs = synthesizedCrossRefs,
        )

        return gson.toJson(attachmentsBackup)
    }

    /**
     * Imports attachments from JSON file
     * Validates context IDs and filters orphaned attachments
     * @param uri URI of the backup file
     * @return Result with import summary or error
     */
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String> {
        val IMPORT_TAG = "AttachmentsRepo_Import"
        try {
            val jsonString = syncFileService.readTextFromUri(uri) ?: throw Exception("File is empty")
            val backupData = gson.fromJson(jsonString, AttachmentsBackup::class.java)
            val existingContextIds = contextDao.getAll().map { it.id }.toSet()

            appDatabase.withTransaction {
                // Import documents
                val validDocs = backupData.documents.filter { it.contextId in existingContextIds }
                noteDocumentDao.insertAllDocuments(validDocs)

                val validDocIds = validDocs.map { it.id }.toSet()
                noteDocumentDao.insertAllDocumentItems(
                    backupData.documentItems.filter { it.listId in validDocIds }
                )

                // Import checklists
                val validChecklists = backupData.checklists.filter { it.contextId in existingContextIds }
                checklistDao.insertChecklists(validChecklists)

                val validChecklistIds = validChecklists.map { it.id }.toSet()
                checklistDao.insertItems(
                    backupData.checklistItems.filter { it.checklistId in validChecklistIds }
                )

                // Import links
                linkItemDao.insertAll(backupData.linkItemEntities)

                // Import attachments with validation
                val processedAttachments = backupData.attachments.map { att ->
                    if (att.ownerContextId != null && att.ownerContextId !in existingContextIds) {
                        att.copy(ownerContextId = null)
                    } else {
                        att
                    }
                }
                attachmentDao.insertAttachments(processedAttachments)

                // Import cross-references
                val attachmentIds = processedAttachments.map { it.id }.toSet()
                val validCrossRefs = backupData.contextAttachmentCrossRefs.filter {
                    it.contextId in existingContextIds && it.attachmentId in attachmentIds
                }
                attachmentDao.insertContextAttachmentLinks(validCrossRefs)
            }

            val orphanCount = backupData.attachments.size -
                    backupData.attachments.count { it.ownerContextId in existingContextIds }

            return Result.success("Імпорт завершено. Знайдено $orphanCount вкладень без прив'язки до проектів.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Critical error during attachments import", e)
            return Result.failure(e)
        }
    }
}