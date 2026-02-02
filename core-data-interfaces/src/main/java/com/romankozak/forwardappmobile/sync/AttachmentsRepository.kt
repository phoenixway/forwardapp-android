// core-data-interfaces/.../sync/AttachmentsRepository.kt
package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import kotlinx.coroutines.flow.Flow

interface AttachmentsRepository {
    // --- Методи синхронізації ---
    suspend fun exportAttachmentsToFile(): Result<String>
    suspend fun createAttachmentsBackupJsonString(): String
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String>

    // --- Методи бізнес-логіки (Додаємо ці методи!) ---
    suspend fun ensureAttachmentLinkedToContext(
        attachmentType: String,
        entityId: String,
        contextId: String,
        ownerContextId: String?,
        createdAt: Long,
        roleCode: String? = null,
        isSystem: Boolean = false
    )
    suspend fun findAttachmentByEntity(attachmentType: String, entityId: String): AttachmentEntity?
    suspend fun deleteAttachment(attachmentId: String)
    fun getAttachmentLibraryItems(): Flow<List<AttachmentLibraryQueryResult>>
    fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>> // ДОДАЙ ЦЕ
    suspend fun linkAttachmentToContext(attachmentId: String, contextId: String) // І ЦЕ

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