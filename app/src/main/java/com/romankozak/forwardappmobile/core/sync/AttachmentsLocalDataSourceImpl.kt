@file:Suppress("TooManyFunctions")

package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.MusicNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

class AttachmentsLocalDataSourceImpl
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val contextDao: ContextDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val musicNoteDao: MusicNoteDao,
        private val checklistDao: ChecklistDao,
        private val linkItemDao: LinkItemDao,
        private val attachmentDao: AttachmentDao,
        private val workspaceDao: WorkspaceDao,
        private val canonicalConnectionsRepository: CanonicalConnectionsRepository,
    ) : AttachmentsLocalDataSource {
        override suspend fun getAttachmentsBackup(): AttachmentsBackup {
            val backup =
                AttachmentsBackup(
                    documents = noteDocumentDao.getAllDocuments(),
                    musicNotes = musicNoteDao.getAll(),
                    checklists = checklistDao.getAllChecklists(),
                    checklistItems = checklistDao.getAllChecklistItems(),
                    linkItemEntities = linkItemDao.getAllEntities(),
                    attachments = attachmentDao.getAll(),
                    contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
                )
            requireNoCanonicalSystemAttachmentCompatibilityPayload(
                attachments = backup.attachments,
                links = backup.contextAttachmentCrossRefs,
            )
            return backup
        }

        override suspend fun importAttachments(backup: AttachmentsBackup): Int {
            requireNoCanonicalSystemAttachmentCompatibilityPayload(
                attachments = backup.attachments,
                links = backup.contextAttachmentCrossRefs,
            )
            val existingContextIds = getAllContextIds()

            appDatabase.withTransaction {
                // Import documents/checklists independently from context links.
                // Their visibility in contexts is defined only by cross-refs.
                noteDocumentDao.insertAllDocuments(backup.documents)
                musicNoteDao.insertAll(backup.musicNotes)
                checklistDao.insertChecklists(backup.checklists)
                val importedChecklistIds = backup.checklists.map { it.id }.toSet()
                checklistDao.insertItems(
                    backup.checklistItems.filter { checklistItem ->
                        checklistItem.checklistId in importedChecklistIds
                    },
                )

                // Імпорт посилань
                linkItemDao.insertAll(backup.linkItemEntities)

                // Import attachments and sanitize invalid owner contexts.
                val finalAttachments =
                    backup.attachments.map { att ->
                        if (att.ownerContextId != null && att.ownerContextId !in existingContextIds) {
                            att.copy(ownerContextId = null)
                        } else {
                            att
                        }
                    }.toMutableList()

                attachmentDao.insertAttachments(finalAttachments)

                val attachmentIds = finalAttachments.map { it.id }.toSet()
                val validCrossRefs =
                    backup.contextAttachmentCrossRefs.filter {
                        it.contextId in existingContextIds && it.attachmentId in attachmentIds
                    }
                attachmentDao.insertContextAttachmentLinks(validCrossRefs)
            }

            return backup.attachments.count { it.ownerContextId != null && it.ownerContextId !in existingContextIds }
        }

        override suspend fun getAllContextIds(): Set<String> {
            return contextDao.getAll().map { it.id }.toSet()
        }

        override suspend fun requireAttachmentPlacementAuthoring(contextId: String) {
            val workspaceId = requireCanonicalOnlySystemWorkspace(contextId)
            if (workspaceId != null) {
                canonicalConnectionsRepository.requireActive(workspaceId)
            }
        }

        override suspend fun findAttachmentByEntity(
            attachmentType: String,
            entityId: String,
        ) = attachmentDao.findAttachmentByEntity(attachmentType, entityId)

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
            isSystem: Boolean,
        ) {
            val systemWorkspaceId = requireCanonicalOnlySystemWorkspace(contextId)
            appDatabase.withTransaction {
                if (systemWorkspaceId != null) {
                    canonicalConnectionsRepository.requireActive(systemWorkspaceId)
                }
                var attachment = attachmentDao.findAttachmentByEntity(attachmentType, entityId)

                if (attachment == null) {
                    attachment =
                        AttachmentEntity(
                            id = UUID.randomUUID().toString(),
                            attachmentType = attachmentType,
                            entityId = entityId,
                            ownerContextId = ownerContextId,
                            createdAt = createdAt,
                            updatedAt = createdAt,
                            version = 1,
                        )
                    attachmentDao.insertAttachment(attachment)
                }

                if (systemWorkspaceId != null) {
                    canonicalConnectionsRepository.linkAttachmentInTransaction(
                        workspaceId = systemWorkspaceId,
                        attachmentId = attachment.id,
                    )
                } else {
                    insertContextBackedAttachmentLink(attachment.id, contextId)
                }
            }
        }

        // AttachmentsLocalDataSourceImpl.kt
        override fun getAttachmentLibraryItems(): Flow<List<AttachmentLibraryQueryResult>> =
            attachmentDao.getLibraryItemsFlow()

        override fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>> =
            attachmentDao.getAllContextAttachmentLinksFlow()

        override suspend fun linkAttachmentToContext(
            attachmentId: String,
            contextId: String,
        ) {
            val systemWorkspaceId = requireCanonicalOnlySystemWorkspace(contextId)
            if (systemWorkspaceId != null) {
                canonicalConnectionsRepository.linkAttachment(
                    workspaceId = systemWorkspaceId,
                    attachmentId = attachmentId,
                )
                return
            }
            appDatabase.withTransaction {
                insertContextBackedAttachmentLink(attachmentId, contextId)
            }
        }

        override fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>> =
            flow {
                val systemWorkspaceId = requireCanonicalOnlySystemWorkspace(contextId)
                emitAll(
                    if (systemWorkspaceId != null) {
                        attachmentDao.getAttachmentsForCanonicalWorkspace(systemWorkspaceId)
                    } else {
                        attachmentDao.getAttachmentsForContext(contextId)
                    },
                )
            }

        override suspend fun getAttachmentById(id: String): AttachmentEntity? = attachmentDao.getAttachmentById(id)

        override suspend fun unlinkAttachmentFromContext(
            attachmentId: String,
            contextId: String,
        ) {
            val systemWorkspaceId = requireCanonicalOnlySystemWorkspace(contextId)
            if (systemWorkspaceId != null) {
                canonicalConnectionsRepository.unlinkAttachment(
                    workspaceId = systemWorkspaceId,
                    attachmentId = attachmentId,
                )
                return
            }
            attachmentDao.deleteContextAttachmentLink(contextId, attachmentId)
        }

        override suspend fun updateAttachmentOrders(
            contextId: String,
            orders: Map<String, Long>,
        ) {
            val systemWorkspaceId = requireCanonicalOnlySystemWorkspace(contextId)
            if (systemWorkspaceId != null) {
                canonicalConnectionsRepository.reorder(
                    workspaceId = systemWorkspaceId,
                    orderedAttachmentIds =
                        orders.entries
                            .sortedWith(compareBy<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
                            .map { it.key },
                )
                return
            }
            appDatabase.withTransaction {
                orders.forEach { (attachmentId, order) ->
                    attachmentDao.updateAttachmentOrder(contextId, attachmentId, order)
                }
            }
        }

        override suspend fun createLinkAttachment(
            contextId: String,
            link: RelatedLink,
            roleCode: String?,
            isSystem: Boolean, // Оновлено
        ): String {
            val systemWorkspaceId = requireCanonicalOnlySystemWorkspace(contextId)
            return appDatabase.withTransaction {
                if (systemWorkspaceId != null) {
                    canonicalConnectionsRepository.requireActive(systemWorkspaceId)
                }
                val linkItemId = UUID.randomUUID().toString()

                linkItemDao.insert(
                    LinkItemEntity(
                        id = linkItemId,
                        linkData = link,
                        createdAt = System.currentTimeMillis(),
                    ),
                )

                val attachment =
                    AttachmentEntity(
                        attachmentType = BacklogItemTypeValues.LINK_ITEM,
                        entityId = linkItemId,
                        ownerContextId = contextId,
                        roleCode = roleCode,
                        isSystem = isSystem, // Використовуємо параметр
                        version = 1,
                    )
                attachmentDao.insertAttachment(attachment)
                if (systemWorkspaceId != null) {
                    canonicalConnectionsRepository.linkAttachmentInTransaction(
                        workspaceId = systemWorkspaceId,
                        attachmentId = attachment.id,
                    )
                } else {
                    insertContextBackedAttachmentLink(attachment.id, contextId)
                }

                attachment.id // Повертаємо String
            }
        }

        override suspend fun findAttachmentByRole(
            contextId: String,
            roleCode: String,
        ): AttachmentEntity? {
            return attachmentDao.findAttachmentByRole(contextId, roleCode)
        }

        /**
         * Returns a direct canonical placement owner for an exact promoted
         * System identity, or null for every ordinary compatibility caller.
         * Invalid System ownership never falls back to sourceContextId routing.
         */
        private suspend fun requireCanonicalOnlySystemWorkspace(ownerId: String): String? {
            if (!SystemContexts.isSystem(ContextId(ownerId))) return null

            val workspace = requireNotNull(workspaceDao.getById(ownerId)) {
                "Canonical System attachment owner $ownerId is missing"
            }
            require(!workspace.isDeleted) {
                "Canonical System attachment owner $ownerId is deleted"
            }
            require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "Canonical System attachment owner $ownerId has invalid provenance"
            }
            require(workspace.sourceContextId == null) {
                "Canonical System attachment owner $ownerId has a legacy Context source"
            }
            return workspace.id
        }

        private suspend fun insertContextBackedAttachmentLink(
            attachmentId: String,
            contextId: String,
        ) {
            attachmentDao.insertContextAttachmentLink(
                ContextAttachmentCrossRef(
                    contextId = contextId,
                    attachmentId = attachmentId,
                ),
            )
        }

        /**
         * The standalone attachment-file format cannot transport canonical
         * Workspace Connections. Refuse exact System ownership rather than
         * emitting/importing a legacy-shaped payload that would lose placement.
         */
        private fun requireNoCanonicalSystemAttachmentCompatibilityPayload(
            attachments: List<AttachmentEntity>,
            links: List<ContextAttachmentCrossRef>,
        ) {
            val hasSystemOwner =
                attachments.any { attachment ->
                    attachment.ownerContextId?.let { SystemContexts.isSystem(ContextId(it)) } == true
                }
            val hasSystemLink = links.any { link -> SystemContexts.isSystem(ContextId(link.contextId)) }
            require(!hasSystemOwner && !hasSystemLink) {
                "Standalone attachment backup cannot represent canonical System Workspace Connections"
            }
        }
    }
