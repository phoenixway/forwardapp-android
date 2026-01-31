package com.romankozak.forwardappmobile.features.attachments.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Transaction
    @Query(
        """
        SELECT a.*, link.context_id AS context_id, link.attachment_order AS attachment_order
        FROM attachments AS a
        INNER JOIN context_attachment_cross_ref AS link
            ON link.attachment_id = a.id
        WHERE link.context_id = :contextId
        ORDER BY link.attachment_order ASC, a.createdAt DESC
        """,
    )
    fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>>

    @Query("SELECT * FROM attachments WHERE id = :attachmentId LIMIT 1")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query(
        """
        SELECT *
        FROM attachments
        WHERE attachment_type = :attachmentType AND entity_id = :entityId
        LIMIT 1
        """,
    )
    suspend fun findAttachmentByEntity(
        attachmentType: String,
        entityId: String,
    ): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextAttachmentLink(link: ContextAttachmentCrossRef)

    @Query(
        """
        DELETE FROM context_attachment_cross_ref
        WHERE context_id = :contextId AND attachment_id = :attachmentId
        """,
    )
    suspend fun deleteContextAttachmentLink(
        contextId: String,
        attachmentId: String,
    )

    @Query("DELETE FROM context_attachment_cross_ref WHERE attachment_id = :attachmentId")
    suspend fun deleteAllLinksForAttachment(attachmentId: String)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    @Query("SELECT COUNT(*) FROM context_attachment_cross_ref WHERE attachment_id = :attachmentId")
    suspend fun countLinksForAttachment(attachmentId: String): Int

    @Query(
        """
        UPDATE context_attachment_cross_ref
        SET attachment_order = :order
        WHERE context_id = :contextId AND attachment_id = :attachmentId
        """,
    )
    suspend fun updateAttachmentOrder(
        contextId: String,
        attachmentId: String,
        order: Long,
    )

    @Query("SELECT * FROM attachments")
    fun getAllAttachmentsFlow(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM context_attachment_cross_ref")
    fun getAllContextAttachmentLinksFlow(): Flow<List<ContextAttachmentCrossRef>>

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>

    @Query("SELECT * FROM context_attachment_cross_ref")
    suspend fun getAllContextAttachmentCrossRefs(): List<ContextAttachmentCrossRef>

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()

    @Query("DELETE FROM context_attachment_cross_ref")
    suspend fun deleteAllContextAttachmentLinks()

    @Query("""
        SELECT 
            a.id AS id,
            a.attachment_type AS attachmentType,
            a.entity_id AS entityId,
            a.owner_context_id AS ownerContextId,
            a.updatedAt AS attachmentUpdatedAt,
            n.name AS noteName, 
            n.updatedAt AS noteUpdatedAt,
            c.name AS checklistName, 
            l.link_data AS linkDisplayName, 
            l.createdAt AS linkCreatedAt,
            ctx.name AS contextName, 
            ctx.updatedAt AS contextUpdatedAt
        FROM attachments AS a
        LEFT JOIN note_documents AS n ON a.attachment_type = 'NOTE_DOCUMENT' AND a.entity_id = n.id
        LEFT JOIN checklists AS c ON a.attachment_type = 'CHECKLIST' AND a.entity_id = c.id
        LEFT JOIN link_items AS l ON a.attachment_type = 'LINK_ITEM' AND a.entity_id = l.id
        LEFT JOIN contexts AS ctx ON a.attachment_type = 'CONTEXT' AND a.entity_id = ctx.id
    """)
    fun getLibraryItemsFlow(): Flow<List<AttachmentLibraryQueryResult>>

    @Query("SELECT * FROM attachments WHERE owner_context_id = :contextId AND role_code = :roleCode LIMIT 1")
    suspend fun findAttachmentByRole(contextId: String, roleCode: String): AttachmentEntity?

    @Query("SELECT * FROM attachments")
    suspend fun getAllRaw(): List<AttachmentEntity>

    @Query("SELECT * FROM context_attachment_cross_ref")
    suspend fun getAllContextAttachmentCrossRefsRaw(): List<ContextAttachmentCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextAttachmentCrossRefs(links: List<ContextAttachmentCrossRef>)
}
