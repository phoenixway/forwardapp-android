package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentLibraryQueryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoOpAttachmentsRepository : AttachmentsRepository {
    override suspend fun exportAttachmentsToFile() = Result.failure(Exception("Disabled"))
    override suspend fun createAttachmentsBackupJsonString() = ""
    override suspend fun importAttachmentsFromFile(uri: Uri) = Result.failure(Exception("Disabled"))
    
    // Повертаємо порожні потоки, щоб UI не падав
    override fun getAttachmentLibraryItems(): Flow<List<AttachmentLibraryQueryResult>> = flowOf(emptyList())
    override fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>> = flowOf(emptyList())
    
    override suspend fun linkAttachmentToContext(attachmentId: String, contextId: String) {}
    override suspend fun ensureAttachmentLinkedToContext(
        t: String, e: String, c: String, o: String?, cr: Long, r: String?, s: Boolean
    ) {}

    override suspend fun findAttachmentByEntity(t: String, e: String): AttachmentEntity? = null
    override suspend fun deleteAttachment(id: String) {}

    override fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>> = flowOf(emptyList())
    override suspend fun getAttachmentById(id: String): AttachmentEntity? = null
    override suspend fun unlinkAttachmentFromContext(attachmentId: String, contextId: String) {}
    override suspend fun updateAttachmentOrders(contextId: String, orders: Map<String, Long>) {}
    override suspend fun createLinkAttachment(
        contextId: String,
        link: RelatedLink,
        roleCode: String?,
        isSystem: Boolean
    ): String = ""    override suspend fun findAttachmentByRole(c: String, r: String): AttachmentEntity? = null
}
