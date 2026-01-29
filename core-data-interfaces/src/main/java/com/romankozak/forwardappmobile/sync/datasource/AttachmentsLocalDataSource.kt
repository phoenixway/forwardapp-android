package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import kotlinx.coroutines.flow.Flow

interface AttachmentsLocalDataSource {
    // --- Методи синхронізації ---
    suspend fun getAttachmentsBackup(): AttachmentsBackup
    suspend fun importAttachments(backup: AttachmentsBackup): Int
    suspend fun getAllContextIds(): Set<String>

    // --- Бізнес-логіка ---
    suspend fun ensureAttachmentLinkedToContext(
        attachmentType: String,
        entityId: String,
        contextId: String,
        ownerContextId: String?,
        createdAt: Long,
        roleCode: String?,
        isSystem: Boolean
    )

    suspend fun findAttachmentByEntity(attachmentType: String, entityId: String): AttachmentEntity?
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun linkAttachmentToContext(attachmentId: String, contextId: String)

    // --- Методи для бібліотеки вкладень (UI) ---
    // Залишаємо лише один варіант, який повертає складний результат для UI
    fun getAttachmentLibraryItems(): Flow<List<AttachmentLibraryQueryResult>>

    fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>>

    fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>>
    suspend fun getAttachmentById(id: String): AttachmentEntity?
    suspend fun unlinkAttachmentFromContext(attachmentId: String, contextId: String)
    suspend fun updateAttachmentOrders(contextId: String, orders: Map<String, Long>)
    suspend fun createLinkAttachment(
        contextId: String,
        link: RelatedLink,
        roleCode: String? = null,
        isSystem: Boolean = false // Додано параметр
    ): String    suspend fun findAttachmentByRole(contextId: String, roleCode: String): AttachmentEntity?
}