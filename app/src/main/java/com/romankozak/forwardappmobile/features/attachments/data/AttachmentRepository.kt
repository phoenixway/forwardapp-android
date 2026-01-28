package com.romankozak.forwardappmobile.features.attachments.data

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

const val ATTACHMENT_LOG_TAG = "FWD_ATTACH"

@Singleton
class AttachmentRepository
    @Inject
    constructor(
        private val attachmentDao: AttachmentDao,
        private val linkItemDao: LinkItemDao,
    ) {
        fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>> =
            attachmentDao.getAttachmentsForContext(contextId)

        fun getAllAttachments(): Flow<List<AttachmentEntity>> = attachmentDao.getAllAttachmentsFlow()

        fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>> = attachmentDao.getAllContextAttachmentLinksFlow()

        fun getAllLinkItems(): Flow<List<LinkItemEntity>> = linkItemDao.getAllEntitiesAsFlow()

        fun getAttachmentLibraryItems(): Flow<List<com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentLibraryQueryResult>> =
            attachmentDao.getAttachmentLibraryItems()

        suspend fun findAttachmentByEntity(
            attachmentType: String,
            entityId: String,
        ): AttachmentEntity? = attachmentDao.findAttachmentByEntity(attachmentType, entityId)

        suspend fun getAttachmentById(attachmentId: String): AttachmentEntity? = attachmentDao.getAttachmentById(attachmentId)

        suspend fun findAttachmentByRole(
            contextId: String,
            roleCode: String,
        ): AttachmentEntity? = attachmentDao.findAttachmentByRole(contextId, roleCode)

        suspend fun ensureAttachmentForEntity(
            attachmentType: String,
            entityId: String,
            ownerContextId: String?,
            createdAt: Long = System.currentTimeMillis(),
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): AttachmentEntity {
            Log.d(
                ATTACHMENT_LOG_TAG,
                "[ensureAttachmentForEntity] START: type=$attachmentType, entity=$entityId, owner=$ownerContextId, createdAt=$createdAt",
            )
            val existing = attachmentDao.findAttachmentByEntity(attachmentType, entityId)
            if (existing != null) {
                Log.d(
                    ATTACHMENT_LOG_TAG,
                    "[ensureAttachmentForEntity] FOUND existing: id=${existing.id}, syncedAt=${existing.syncedAt}, version=${existing.version}",
                )
                val needsUpdate = (roleCode != null && existing.roleCode != roleCode) || (isSystem && !existing.isSystem)
                return if (needsUpdate) {
                    val updated =
                        existing.copy(
                            roleCode = roleCode ?: existing.roleCode,
                            isSystem = existing.isSystem || isSystem,
                            updatedAt = System.currentTimeMillis(),
                            version = existing.version + 1,
                        )
                    attachmentDao.insertAttachment(updated)
                    updated
                } else {
                    existing
                }
            }

            val attachment =
                AttachmentEntity(
                    id = UUID.randomUUID().toString(),
                    attachmentType = attachmentType,
                    entityId = entityId,
                    ownerContextId = ownerContextId,
                    roleCode = roleCode,
                    isSystem = isSystem,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    syncedAt = null,
                    version = 1,
                )
            attachmentDao.insertAttachment(attachment)
            Log.d(
                ATTACHMENT_LOG_TAG,
                "[ensureAttachmentForEntity] CREATED: id=${attachment.id}, type=$attachmentType, entity=$entityId, version=1, syncedAt=null",
            )
            return attachment
        }

        suspend fun ensureAttachmentLinkedToContext(
            attachmentType: String,
            entityId: String,
            contextId: String,
            ownerContextId: String? = null,
            createdAt: Long = System.currentTimeMillis(),
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): AttachmentEntity {
            Log.d(ATTACHMENT_LOG_TAG, "[ensureAttachmentLinkedToContext] START: type=$attachmentType, entity=$entityId, context=$contextId")
            val attachment =
                ensureAttachmentForEntity(
                    attachmentType,
                    entityId,
                    ownerContextId,
                    createdAt,
                    roleCode,
                    isSystem,
                )

            // Check if this link already exists to prevent duplicates
            val existingLink = attachmentDao.getContextAttachmentLink(contextId, attachment.id)
            if (existingLink == null) {
                attachmentDao.insertContextAttachmentLink(
                    ContextAttachmentCrossRef(
                        contextId = contextId,
                        attachmentId = attachment.id,
                        attachmentOrder = -createdAt,
                    ),
                )
                Log.d(ATTACHMENT_LOG_TAG, "[ensureAttachmentLinkedToContext] LINKED: attachment=${attachment.id} -> context=$contextId")
            } else {
                Log.d(
                    ATTACHMENT_LOG_TAG,
                    "[ensureAttachmentLinkedToContext] ALREADY LINKED: attachment=${attachment.id} -> context=$contextId",
                )
            }
            return attachment
        }

        suspend fun createLinkAttachment(
            contextId: String,
            link: RelatedLink,
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): AttachmentEntity {
            val timestamp = System.currentTimeMillis()
            Log.d(
                ATTACHMENT_LOG_TAG,
                "[createLinkAttachment] START: context=$contextId, link=${link.displayName ?: link.target}, ts=$timestamp",
            )

            val newAttachment =
                ensureAttachmentForEntity(
                    attachmentType = BacklogItemTypeValues.LINK_ITEM,
                    entityId = link.target,
                    ownerContextId = contextId,
                    createdAt = timestamp,
                    roleCode = roleCode,
                    isSystem = isSystem,
                )
            Log.d(
                ATTACHMENT_LOG_TAG,
                "[createLinkAttachment] STEP2: Attachment ensured: id=${newAttachment.id}, type=LINK_ITEM, entityId=${link.target}, owner=$contextId, version=${newAttachment.version}, syncedAt=${newAttachment.syncedAt}",
            )

            attachmentDao.insertContextAttachmentLink(
                ContextAttachmentCrossRef(
                    contextId = contextId,
                    attachmentId = newAttachment.id,
                    attachmentOrder = -timestamp,
                    updatedAt = timestamp,
                    syncedAt = null,
                    version = 1,
                ),
            )
            Log.d(
                ATTACHMENT_LOG_TAG,
                "[createLinkAttachment] STEP3: ContextAttachmentCrossRef created: context=$contextId, attachment=${newAttachment.id}, version=1, syncedAt=null (NEW - WILL NEED SYNC)",
            )
            Log.d(
                ATTACHMENT_LOG_TAG,
                "[createLinkAttachment] DONE: attachment=${newAttachment.id}, this attachment is NEW and unsync'd (syncedAt=null), it will be exported on next sync",
            )
            return newAttachment
        }

        suspend fun linkAttachmentToContext(
            attachmentId: String,
            contextId: String,
            order: Long = -System.currentTimeMillis(),
        ) {
            attachmentDao.insertContextAttachmentLink(
                ContextAttachmentCrossRef(
                    contextId = contextId,
                    attachmentId = attachmentId,
                    attachmentOrder = order,
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = 1,
                ),
            )
        }

        suspend fun unlinkAttachmentFromContext(
            attachmentId: String,
            contextId: String,
        ): Boolean {
            val existing = attachmentDao.getAttachmentById(attachmentId) ?: return false
            val now = System.currentTimeMillis()
            val link = attachmentDao.getContextAttachmentLink(contextId, attachmentId)
            if (link != null) {
                attachmentDao.insertContextAttachmentLink(
                    link.softDelete(now),
                )
            } else {
                attachmentDao.deleteContextAttachmentLink(contextId, attachmentId)
            }
            val remainingLinks = attachmentDao.countLinksForAttachment(attachmentId)
            val noMoreLinks = remainingLinks <= 0
            if (noMoreLinks) {
                attachmentDao.insertAttachment(
                    existing.softDelete(now),
                )
                if (existing.attachmentType == BacklogItemTypeValues.LINK_ITEM) {
                    linkItemDao.deleteById(existing.entityId)
                }
            }
            return noMoreLinks
        }

        suspend fun deleteAttachment(attachmentId: String) {
            val now = System.currentTimeMillis()
            val existing = attachmentDao.getAttachmentById(attachmentId)
            if (existing != null) {
                attachmentDao.insertAttachment(
                    existing.softDelete(now),
                )
                // mark links deleted too
                val links = attachmentDao.getContextAttachmentLinksForAttachment(attachmentId)
                links.forEach { link ->
                    attachmentDao.insertContextAttachmentLink(
                        link.softDelete(now),
                    )
                }
                if (existing.attachmentType == BacklogItemTypeValues.LINK_ITEM) {
                    linkItemDao.deleteById(existing.entityId)
                }
            } else {
                attachmentDao.deleteAllLinksForAttachment(attachmentId)
                attachmentDao.deleteAttachment(attachmentId)
            }
        }

        suspend fun updateAttachmentOrders(
            contextId: String,
            updates: List<Pair<String, Long>>,
        ) {
            if (updates.isEmpty()) return
            updates.forEach { (attachmentId, order) ->
                val now = System.currentTimeMillis()
                val existing = attachmentDao.getContextAttachmentLink(contextId, attachmentId)
                if (existing != null) {
                    attachmentDao.insertContextAttachmentLink(
                        existing.copy(
                            attachmentOrder = order,
                            updatedAt = now,
                            syncedAt = null,
                            version = existing.version + 1,
                        ),
                    )
                } else {
                    attachmentDao.updateAttachmentOrder(contextId, attachmentId, order)
                }
            }
        }
    }
