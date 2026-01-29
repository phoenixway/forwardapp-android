// core-data-interfaces/.../datasource/AttachmentsLocalDataSource.kt
package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup

interface AttachmentsLocalDataSource {
    // --- Методи синхронізації ---
    suspend fun getAttachmentsBackup(): AttachmentsBackup
    suspend fun importAttachments(backup: AttachmentsBackup): Int

    // --- БІЗНЕС-ЛОГІКА (Додаємо ці методи) ---
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
    suspend fun getAllContextIds(): Set<String> // Додайте цей рядок
}