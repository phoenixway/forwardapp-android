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
    /** Read-only inverse links. Workspace relations use canonical owners; legacy beacon ids
     * are resolved only through an explicit live CUT_OVER mapping, never by title/id guessing. */
    @Query("""
        WITH checklist_attachments AS (
            SELECT a.id FROM attachments a JOIN checklists k ON k.id = a.entity_id
            WHERE a.attachment_type = 'CHECKLIST' AND a.entity_id = :checklistId
              AND a.isDeleted = 0 AND k.isDeleted = 0
        )
        SELECT DISTINCT 'WORKSPACE' AS kind, w.id AS ownerId,
            COALESCE(NULLIF(w.nameOverride, ''), w.id) AS title,
            'WORKSPACE' AS destinationKind, w.id AS destinationId
        FROM workspace_connections c
        JOIN checklist_attachments a ON a.id = c.attachmentId
        JOIN workspaces w ON w.id = c.workspaceId AND w.isDeleted = 0
        JOIN workspace_capability_instances cap ON cap.id = c.capabilityInstanceId
            AND cap.workspaceId = w.id AND cap.isDeleted = 0
        WHERE c.isDeleted = 0
        UNION
        SELECT DISTINCT 'PROBLEM', p.id, p.title, 'WORKSPACE', w.id
        FROM workspace_problem_attachment_refs r
        JOIN checklist_attachments a ON a.id = r.attachmentId
        JOIN workspace_problems p ON p.id = r.problemId AND p.isDeleted = 0
        JOIN workspaces w ON w.id = p.workspaceId AND w.isDeleted = 0
        JOIN workspace_capability_instances cap ON cap.id = p.capabilityInstanceId
            AND cap.workspaceId = w.id AND cap.isDeleted = 0
        WHERE r.isDeleted = 0
        UNION
        SELECT DISTINCT 'MISSION', CAST(m.id AS TEXT), m.title,
            CASE WHEN pc.id IS NULL THEN 'TACTICS' ELSE 'PARTICIPATION' END,
            COALESCE(pc.id, CAST(m.id AS TEXT))
        FROM tactical_mission_attachment_cross_ref r
        JOIN checklist_attachments a ON a.id = r.attachmentId
        JOIN tactical_missions m ON m.id = r.missionId AND m.is_deleted = 0
        LEFT JOIN planning_commitments pc ON pc.id = (
            SELECT candidate.id FROM planning_commitments candidate
            JOIN planning_scopes scope ON scope.id = candidate.scopeId AND scope.isDeleted = 0
                AND scope.kind = 'TACTICAL_CYCLE'
            JOIN managed_subjects subject ON subject.id = candidate.orientationId AND subject.isDeleted = 0
            WHERE candidate.provenanceSourceType = 'TACTICAL_MISSION'
                AND candidate.provenanceSourceId = CAST(m.id AS TEXT)
                AND candidate.role = 'MISSION' AND candidate.isDeleted = 0
            ORDER BY candidate.updatedAt DESC, candidate.id LIMIT 1
        )
        UNION
        SELECT DISTINCT 'BEACON', b.id, subject.title, 'SEMANTIC', subject.id
        FROM main_beacon_attachment_cross_ref r
        JOIN checklist_attachments a ON a.id = r.attachment_id
        JOIN main_beacons b ON b.id = r.beacon_id
        JOIN legacy_subject_mappings mapping ON mapping.sourceType = 'MAIN_BEACON'
            AND mapping.sourceId = b.id AND mapping.isDeleted = 0 AND mapping.state = 'CUT_OVER'
        JOIN managed_subjects subject ON subject.id = mapping.subjectId AND subject.isDeleted = 0
        ORDER BY kind, title COLLATE NOCASE, ownerId
    """)
    abstract fun observeChecklistUsages(checklistId: String): Flow<List<ChecklistUsageRow>>

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

    /**
     * Canonical-only owners have no source Context id to use for the legacy
     * compatibility projection. The returned context id is the stable owner
     * Workspace id, not a manufactured Context row.
     */
    @Transaction
    @Query(
        """
        SELECT a.*, c.workspaceId AS context_id, c.connectionOrder AS attachment_order
        FROM attachments AS a
        INNER JOIN workspace_connections AS c ON c.attachmentId = a.id
        INNER JOIN workspaces AS w ON w.id = c.workspaceId
        WHERE c.workspaceId = :workspaceId
          AND w.isDeleted = 0
          AND c.isDeleted = 0
          AND a.isDeleted = 0
        ORDER BY c.connectionOrder ASC, a.createdAt DESC
        """,
    )
    abstract fun getAttachmentsForCanonicalWorkspace(workspaceId: String): Flow<List<AttachmentWithContext>>

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
        LEFT JOIN note_documents AS n ON a.attachment_type = 'NOTE_DOCUMENT' AND a.entity_id = n.id
        LEFT JOIN music_notes AS mn ON a.attachment_type = 'MUSIC_NOTE' AND a.entity_id = mn.id
        LEFT JOIN checklists AS c ON a.attachment_type = 'CHECKLIST' AND a.entity_id = c.id
        LEFT JOIN link_items AS l ON a.attachment_type = 'LINK_ITEM' AND a.entity_id = l.id
        LEFT JOIN scripts AS s ON a.attachment_type = 'SCRIPT' AND a.entity_id = s.id
        LEFT JOIN workspace_connections AS connection ON a.id = connection.attachmentId AND connection.isDeleted = 0
        LEFT JOIN workspaces AS workspace ON connection.workspaceId = workspace.id
        LEFT JOIN contexts AS linked_ctx ON workspace.sourceContextId = linked_ctx.id
        WHERE a.isDeleted = 0
          AND (a.attachment_type != 'NOTE_DOCUMENT' OR (n.id IS NOT NULL AND n.isDeleted = 0))
          AND (a.attachment_type != 'MUSIC_NOTE' OR (mn.id IS NOT NULL AND mn.isDeleted = 0))
          AND (a.attachment_type != 'CHECKLIST' OR (c.id IS NOT NULL AND c.isDeleted = 0))
          AND (a.attachment_type != 'SCRIPT' OR (s.id IS NOT NULL AND s.isDeleted = 0))
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

/** Transient read projection, not a persisted ownership model. */
data class ChecklistUsageRow(
    val kind: String,
    val ownerId: String,
    val title: String,
    val destinationKind: String,
    val destinationId: String,
)
