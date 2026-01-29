// core-data-interfaces/.../sync/AttachmentsRepository.kt
package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity

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
}