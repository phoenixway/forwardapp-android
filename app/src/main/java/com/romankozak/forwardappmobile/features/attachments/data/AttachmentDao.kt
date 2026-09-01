package com.romankozak.forwardappmobile.features.attachments.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AttachmentDao {
    @Transaction
    @Query(
        """
        SELECT a.*, w.sourceContextId AS context_id, c.connectionOrder AS attachment_order
        FROM attachments AS a
        INNER JOIN workspace_connections AS c ON c.attachmentId = a.id
        INNER JOIN workspaces AS w ON w.id = c.workspaceId
        WHERE w.sourceContextId = :contextId
          AND c.isDeleted = 0
          AND a.isDeleted = 0
        ORDER BY c.connectionOrder ASC, a.createdAt DESC
        """,
    )
    abstract fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>>

    @Query("SELECT * FROM attachments WHERE id = :attachmentId LIMIT 1")
    abstract suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query(
        """
        SELECT *
        FROM attachments
        WHERE attachment_type = :attachmentType AND entity_id = :entityId
        LIMIT 1
        """,
    )
    abstract suspend fun findAttachmentByEntity(
        attachmentType: String,
        entityId: String,
    ): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Transaction
    open suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>) {
        links.forEach { insertContextAttachmentLink(it) }
    }

    @Transaction
    open suspend fun insertContextAttachmentLink(link: ContextAttachmentCrossRef) {
        upsertContextAttachmentLink(
            contextId = link.contextId,
            attachmentId = link.attachmentId,
            order = link.attachmentOrder,
            updatedAt = link.updatedAt ?: System.currentTimeMillis(),
            syncedAt = link.syncedAt,
            isDeleted = link.isDeleted,
            version = link.version,
        )
    }

    @Query(
        """
        INSERT INTO workspace_connections (
            id, workspaceId, capabilityInstanceId, attachmentId, connectionOrder,
            createdAt, updatedAt, syncedAt, isDeleted, version
        )
        SELECT
            'WORKSPACE_CONNECTION:' || length(cap.id) || ':' || cap.id || ':' ||
                length(:attachmentId) || ':' || :attachmentId,
            w.id,
            cap.id,
            :attachmentId,
            :order,
            0,
            :updatedAt,
            :syncedAt,
            :isDeleted,
            :version
        FROM workspaces w
        JOIN workspace_capability_instances cap
          ON cap.workspaceId = w.id
         AND cap.capabilityType = 'CONNECTIONS'
         AND cap.instanceKey = 'default'
         AND cap.isDeleted = 0
        WHERE w.sourceContextId = :contextId
          AND w.isDeleted = 0
        ON CONFLICT(id) DO UPDATE SET
            connectionOrder = excluded.connectionOrder,
            updatedAt = excluded.updatedAt,
            syncedAt = excluded.syncedAt,
            isDeleted = excluded.isDeleted,
            version = excluded.version
        """,
    )
    protected abstract suspend fun upsertContextAttachmentLink(
        contextId: String,
        attachmentId: String,
        order: Long,
        updatedAt: Long,
        syncedAt: Long?,
        isDeleted: Boolean,
        version: Long,
    )

    @Query(
        """
        UPDATE workspace_connections
        SET isDeleted = 1,
            updatedAt = :now,
            syncedAt = NULL,
            version = version + 1
        WHERE attachmentId = :attachmentId
          AND workspaceId = (
              SELECT id FROM workspaces WHERE sourceContextId = :contextId LIMIT 1
          )
          AND isDeleted = 0
        """,
    )
    abstract suspend fun deleteContextAttachmentLink(
        contextId: String,
        attachmentId: String,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE workspace_connections
        SET isDeleted = 1,
            updatedAt = :now,
            syncedAt = NULL,
            version = version + 1
        WHERE attachmentId = :attachmentId AND isDeleted = 0
        """,
    )
    abstract suspend fun deleteAllLinksForAttachment(
        attachmentId: String,
        now: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    abstract suspend fun deleteAttachment(attachmentId: String)

    @Query("SELECT COUNT(*) FROM workspace_connections WHERE attachmentId = :attachmentId AND isDeleted = 0")
    abstract suspend fun countLinksForAttachment(attachmentId: String): Int

    @Query(
        """
        UPDATE workspace_connections
        SET connectionOrder = :order,
            updatedAt = :now,
            syncedAt = NULL,
            version = version + 1
        WHERE attachmentId = :attachmentId
          AND workspaceId = (
              SELECT id FROM workspaces WHERE sourceContextId = :contextId LIMIT 1
          )
          AND isDeleted = 0
        """,
    )
    abstract suspend fun updateAttachmentOrder(
        contextId: String,
        attachmentId: String,
        order: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query("SELECT * FROM attachments")
    abstract fun getAllAttachmentsFlow(): Flow<List<AttachmentEntity>>

    @Query(
        """
        SELECT w.sourceContextId AS context_id,
               c.attachmentId AS attachment_id,
               c.connectionOrder AS attachment_order,
               c.updatedAt AS updatedAt,
               c.syncedAt AS syncedAt,
               c.isDeleted AS isDeleted,
               c.version AS version
        FROM workspace_connections c
        JOIN workspaces w ON w.id = c.workspaceId
        WHERE w.sourceContextId IS NOT NULL
        """,
    )
    abstract fun getAllContextAttachmentLinksFlow(): Flow<List<ContextAttachmentCrossRef>>

    @Query("SELECT * FROM attachments")
    abstract suspend fun getAll(): List<AttachmentEntity>

    @Query(
        """
        SELECT w.sourceContextId AS context_id,
               c.attachmentId AS attachment_id,
               c.connectionOrder AS attachment_order,
               c.updatedAt AS updatedAt,
               c.syncedAt AS syncedAt,
               c.isDeleted AS isDeleted,
               c.version AS version
        FROM workspace_connections c
        JOIN workspaces w ON w.id = c.workspaceId
        WHERE w.sourceContextId IS NOT NULL
        """,
    )
    abstract suspend fun getAllContextAttachmentCrossRefs(): List<ContextAttachmentCrossRef>

    @Query("DELETE FROM attachments")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM workspace_connections")
    abstract suspend fun deleteAllContextAttachmentLinks()

    @Query(
        """
        SELECT
            a.id AS id,
            a.attachment_type AS attachmentType,
            a.entity_id AS entityId,
            a.owner_context_id AS ownerContextId,
            a.updatedAt AS attachmentUpdatedAt,
            n.name AS noteName,
            n.content AS noteContent,
            mn.name AS musicNoteName,
            mn.content AS musicNoteContent,
            n.updatedAt AS noteUpdatedAt,
            c.name AS checklistName,
            (
                SELECT GROUP_CONCAT(ci.content, char(10))
                FROM checklist_items AS ci
                WHERE ci.checklistId = c.id AND ci.isDeleted = 0
            ) AS checklistContent,
            l.link_data AS linkDisplayName,
            NULL as linkTarget,
            l.createdAt AS linkCreatedAt,
            s.name AS scriptName,
            s.description AS scriptDescription,
            s.content AS scriptContent,
            linked_ctx.name AS contextName,
            linked_ctx.updatedAt AS contextUpdatedAt
        FROM attachments AS a
        LEFT JOIN note_documents AS n ON a.attachment_type IN ('NOTE_DOCUMENT', 'JOURNAL_DOCUMENT') AND a.entity_id = n.id
        LEFT JOIN music_notes AS mn ON a.attachment_type = 'MUSIC_NOTE' AND a.entity_id = mn.id
        LEFT JOIN checklists AS c ON a.attachment_type = 'CHECKLIST' AND a.entity_id = c.id
        LEFT JOIN link_items AS l ON a.attachment_type = 'LINK_ITEM' AND a.entity_id = l.id
        LEFT JOIN scripts AS s ON a.attachment_type = 'SCRIPT' AND a.entity_id = s.id
        LEFT JOIN workspace_connections AS connection ON a.id = connection.attachmentId AND connection.isDeleted = 0
        LEFT JOIN workspaces AS workspace ON connection.workspaceId = workspace.id
        LEFT JOIN contexts AS linked_ctx ON workspace.sourceContextId = linked_ctx.id
        GROUP BY a.id
        """,
    )
    abstract fun getLibraryItemsFlow(): Flow<List<AttachmentLibraryQueryResult>>

    @Query("SELECT * FROM attachments WHERE owner_context_id = :contextId AND role_code = :roleCode LIMIT 1")
    abstract suspend fun findAttachmentByRole(
        contextId: String,
        roleCode: String,
    ): AttachmentEntity?

    @Query("SELECT * FROM attachments")
    abstract suspend fun getAllRaw(): List<AttachmentEntity>

    @Query(
        """
        SELECT w.sourceContextId AS context_id,
               c.attachmentId AS attachment_id,
               c.connectionOrder AS attachment_order,
               c.updatedAt AS updatedAt,
               c.syncedAt AS syncedAt,
               c.isDeleted AS isDeleted,
               c.version AS version
        FROM workspace_connections c
        JOIN workspaces w ON w.id = c.workspaceId
        WHERE w.sourceContextId IS NOT NULL
        """,
    )
    abstract suspend fun getAllContextAttachmentCrossRefsRaw(): List<ContextAttachmentCrossRef>

    @Transaction
    open suspend fun insertContextAttachmentCrossRefs(links: List<ContextAttachmentCrossRef>) {
        insertContextAttachmentLinks(links)
    }
}
