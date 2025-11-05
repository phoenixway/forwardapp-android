package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.database.models.AttachmentEntity
import com.romankozak.forwardappmobile.data.database.models.AttachmentWithProject
import com.romankozak.forwardappmobile.data.database.models.ProjectAttachmentCrossRef
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
}
