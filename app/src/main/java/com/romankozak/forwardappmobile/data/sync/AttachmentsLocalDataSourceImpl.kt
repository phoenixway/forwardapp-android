package com.romankozak.forwardappmobile.data.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import java.util.UUID
import javax.inject.Inject

class AttachmentsLocalDataSourceImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val contextDao: ContextDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val checklistDao: ChecklistDao,
    private val linkItemDao: LinkItemDao,
    private val attachmentDao: AttachmentDao,
) : AttachmentsLocalDataSource {

    override suspend fun getAttachmentsBackup(): AttachmentsBackup {
        return AttachmentsBackup(
            documents = noteDocumentDao.getAllDocuments(),
            documentItems = noteDocumentDao.getAllDocumentItems(),
            checklists = checklistDao.getAllChecklists(),
            checklistItems = checklistDao.getAllChecklistItems(),
            linkItemEntities = linkItemDao.getAllEntities(),
            attachments = attachmentDao.getAll(),
            contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
        )
    }

    override suspend fun importAttachments(backup: AttachmentsBackup): Int {
        val existingContextIds = getAllContextIds()

        appDatabase.withTransaction {
            // Імпорт документів
            val validDocs = backup.documents.filter { it.contextId in existingContextIds }
            noteDocumentDao.insertAllDocuments(validDocs)
            val validDocIds = validDocs.map { it.id }.toSet()
            noteDocumentDao.insertAllDocumentItems(
                backup.documentItems.filter { it.listId in validDocIds }
            )

            // Імпорт чеклистів
            val validChecklists = backup.checklists.filter { it.contextId in existingContextIds }
            checklistDao.insertChecklists(validChecklists)
            val validChecklistIds = validChecklists.map { it.id }.toSet()
            checklistDao.insertItems(
                backup.checklistItems.filter { it.checklistId in validChecklistIds }
            )

            // Імпорт посилань
            linkItemDao.insertAll(backup.linkItemEntities)

            // Імпорт вкладень
            val processedAttachments = backup.attachments.map { att ->
                if (att.ownerContextId != null && att.ownerContextId !in existingContextIds) {
                    att.copy(ownerContextId = null)
                } else {
                    att
                }
            }
            attachmentDao.insertAttachments(processedAttachments)

            // Імпорт зв'язків
            val attachmentIds = processedAttachments.map { it.id }.toSet()
            val validCrossRefs = backup.contextAttachmentCrossRefs.filter {
                it.contextId in existingContextIds && it.attachmentId in attachmentIds
            }
            attachmentDao.insertContextAttachmentLinks(validCrossRefs)
        }

        return backup.attachments.count { it.ownerContextId != null && it.ownerContextId !in existingContextIds }
    }

    override suspend fun getAllContextIds(): Set<String> {
        return contextDao.getAll().map { it.id }.toSet()
    }

    override suspend fun findAttachmentByEntity(attachmentType: String, entityId: String) =
        attachmentDao.findAttachmentByEntity(attachmentType, entityId)

    override suspend fun deleteAttachment(attachmentId: String) {
        appDatabase.withTransaction { // Виправлено: appDatabase замість db
            attachmentDao.deleteAllLinksForAttachment(attachmentId)
            attachmentDao.deleteAttachment(attachmentId)
        }
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
        appDatabase.withTransaction { // Виправлено: appDatabase замість db
            var attachment = attachmentDao.findAttachmentByEntity(attachmentType, entityId)

            if (attachment == null) {
                attachment = AttachmentEntity(
                    id = UUID.randomUUID().toString(),
                    attachmentType = attachmentType,
                    entityId = entityId,
                    ownerContextId = ownerContextId,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    version = 1
                )
                attachmentDao.insertAttachment(attachment)
            }

            // Створюємо зв'язок (CrossRef)
            // УВАГА: roleCode та isSystem видалено, бо їх немає в конструкторі моделі
            val link = ContextAttachmentCrossRef(
                contextId = contextId,
                attachmentId = attachment.id,
                syncedAt = null,
                version = 1
            )
            attachmentDao.insertContextAttachmentLink(link)
        }
    }
}