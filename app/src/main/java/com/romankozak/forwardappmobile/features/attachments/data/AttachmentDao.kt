package com.romankozak.forwardappmobile.features.attachments.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.features.attachments.data.models.ContextAttachmentCrossRef
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
        SELECT a.*
          FROM attachments a
          INNER JOIN context_attachment_cross_ref link ON link.attachment_id = a.id
         WHERE link.context_id = :contextId
           AND a.role_code = :roleCode
           AND a.isDeleted = 0
         LIMIT 1
        """,
    )
    suspend fun findAttachmentByRole(
        contextId: String,
        roleCode: String,
    ): AttachmentEntity?

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
        SELECT * FROM context_attachment_cross_ref
        WHERE context_id = :contextId AND attachment_id = :attachmentId
        LIMIT 1
        """,
    )
    suspend fun getContextAttachmentLink(
        contextId: String,
        attachmentId: String,
    ): ContextAttachmentCrossRef?

    @Query("SELECT * FROM context_attachment_cross_ref WHERE attachment_id = :attachmentId")
    suspend fun getContextAttachmentLinksForAttachment(attachmentId: String): List<ContextAttachmentCrossRef>

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

    @Transaction
    @Query(
        """
        SELECT
            a.id,
            a.entity_id as entityId,
            a.attachment_type as attachmentType,
            a.owner_project_id as ownerProjectId,
            a.updatedAt as attachmentUpdatedAt,
            nd.name as noteName,
            nd.updatedAt as noteUpdatedAt,
            cl.name as checklistName,
            li.link_data as linkDisplayName,
            li.link_data as linkTarget,
            li.createdAt as linkCreatedAt
        FROM attachments as a
        LEFT JOIN note_documents as nd ON a.entity_id = nd.id AND a.attachment_type = 'NOTE_DOCUMENT'
        LEFT JOIN checklists as cl ON a.entity_id = cl.id AND a.attachment_type = 'CHECKLIST'
        LEFT JOIN link_items as li ON a.entity_id = li.id AND a.attachment_type = 'LINK_ITEM'
        """,
    )
    fun getAttachmentLibraryItems(): Flow<List<com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentLibraryQueryResult>>
}
