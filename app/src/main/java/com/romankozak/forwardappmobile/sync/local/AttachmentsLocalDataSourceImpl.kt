package com.romankozak.forwardappmobile.sync.local

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.sync.AttachmentsBackup
import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
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
            // Import documents
            val validDocs = backup.documents.filter { it.contextId in existingContextIds }
            noteDocumentDao.insertAllDocuments(validDocs)
            val validDocIds = validDocs.map { it.id }.toSet()
            noteDocumentDao.insertAllDocumentItems(
                backup.documentItems.filter { it.listId in validDocIds }
            )

            // Import checklists
            val validChecklists = backup.checklists.filter { it.contextId in existingContextIds }
            checklistDao.insertChecklists(validChecklists)
            val validChecklistIds = validChecklists.map { it.id }.toSet()
            checklistDao.insertItems(
                backup.checklistItems.filter { it.checklistId in validChecklistIds }
            )

            // Import links
            linkItemDao.insertAll(backup.linkItemEntities)

            // Import attachments with validation
            val processedAttachments = backup.attachments.map { att ->
                if (att.ownerContextId != null && att.ownerContextId !in existingContextIds) {
                    att.copy(ownerContextId = null)
                } else {
                    att
                }
            }
            attachmentDao.insertAttachments(processedAttachments)

            // Import cross-references
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
}
