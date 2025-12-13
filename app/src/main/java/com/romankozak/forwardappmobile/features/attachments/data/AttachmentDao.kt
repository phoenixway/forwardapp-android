package com.romankozak.forwardappmobile.features.attachments.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentWithProject
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Transaction
    @Query(
        """
        SELECT a.*, link.project_id AS project_id, link.attachment_order AS attachment_order
        FROM attachments AS a
        INNER JOIN project_attachment_cross_ref AS link
            ON link.attachment_id = a.id
        WHERE link.project_id = :projectId
        ORDER BY link.attachment_order ASC, a.createdAt DESC
        """,
    )
    fun getAttachmentsForProject(projectId: String): Flow<List<AttachmentWithProject>>

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
    suspend fun insertProjectAttachmentLinks(links: List<ProjectAttachmentCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectAttachmentLink(link: ProjectAttachmentCrossRef)

    @Query(
        """
        SELECT a.*
          FROM attachments a
          INNER JOIN project_attachment_cross_ref link ON link.attachment_id = a.id
         WHERE link.project_id = :projectId
           AND a.role_code = :roleCode
           AND a.isDeleted = 0
         LIMIT 1
        """
    )
    suspend fun findAttachmentByRole(
        projectId: String,
        roleCode: String
    ): AttachmentEntity?

    @Query(
        """
        DELETE FROM project_attachment_cross_ref
        WHERE project_id = :projectId AND attachment_id = :attachmentId
        """,
    )
    suspend fun deleteProjectAttachmentLink(
        projectId: String,
        attachmentId: String,
    )

    @Query("DELETE FROM project_attachment_cross_ref WHERE attachment_id = :attachmentId")
    suspend fun deleteAllLinksForAttachment(attachmentId: String)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    @Query("SELECT COUNT(*) FROM project_attachment_cross_ref WHERE attachment_id = :attachmentId")
    suspend fun countLinksForAttachment(attachmentId: String): Int

    @Query(
        """
        SELECT * FROM project_attachment_cross_ref
        WHERE project_id = :projectId AND attachment_id = :attachmentId
        LIMIT 1
        """,
    )
    suspend fun getProjectAttachmentLink(
        projectId: String,
        attachmentId: String,
    ): ProjectAttachmentCrossRef?

    @Query("SELECT * FROM project_attachment_cross_ref WHERE attachment_id = :attachmentId")
    suspend fun getProjectAttachmentLinksForAttachment(attachmentId: String): List<ProjectAttachmentCrossRef>

    @Query(
        """
        UPDATE project_attachment_cross_ref
        SET attachment_order = :order
        WHERE project_id = :projectId AND attachment_id = :attachmentId
        """,
    )
    suspend fun updateAttachmentOrder(
        projectId: String,
        attachmentId: String,
        order: Long,
    )

    @Query("SELECT * FROM attachments")
    fun getAllAttachmentsFlow(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM project_attachment_cross_ref")
    fun getAllProjectAttachmentLinksFlow(): Flow<List<ProjectAttachmentCrossRef>>

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>

    @Query("SELECT * FROM project_attachment_cross_ref")
    suspend fun getAllProjectAttachmentCrossRefs(): List<ProjectAttachmentCrossRef>

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()

    @Query("DELETE FROM project_attachment_cross_ref")
    suspend fun deleteAllProjectAttachmentLinks()

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
