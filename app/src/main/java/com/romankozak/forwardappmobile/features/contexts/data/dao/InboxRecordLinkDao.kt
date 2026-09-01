package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecordLink

@Dao
interface InboxRecordLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<InboxRecordLink>)

    @Query("DELETE FROM inbox_record_links WHERE record_id = :recordId AND context_id IN (:contextIds)")
    suspend fun deleteForRecordAndContexts(recordId: String, contextIds: List<String>)

    @Query("DELETE FROM inbox_record_links WHERE record_id = :recordId")
    suspend fun deleteForRecord(recordId: String)

    @Query("DELETE FROM inbox_record_links")
    suspend fun deleteAll()

    @Query("SELECT * FROM inbox_record_links WHERE record_id = :recordId")
    suspend fun getLinksForRecord(recordId: String): List<InboxRecordLink>

    @Query(
        """
        SELECT ir.id, COALESCE(w.sourceContextId, ir.workspaceId) AS contextId,
               ir.text, ir.createdAt, ir.recordOrder AS item_order,
               ir.updatedAt, ir.syncedAt AS synced_at, ir.isDeleted AS is_deleted,
               0 AS hide_in_owner_inbox, ir.version
        FROM workspace_inbox_records ir
        LEFT JOIN workspaces w ON w.id = ir.workspaceId
        JOIN workspace_capability_instances ci ON ci.id = ir.capabilityInstanceId
        WHERE ir.workspaceId = :contextId
          AND ir.isDeleted = 0
          AND (
            ci.configuration != '{"ownerVisibility":"HIDE_WHEN_ASSOCIATED"}'
            OR NOT EXISTS (
              SELECT 1
              FROM inbox_record_links owner_links
              WHERE owner_links.record_id = ir.id
            )
          )
        UNION
        SELECT ir.id, COALESCE(w.sourceContextId, ir.workspaceId) AS contextId,
               ir.text, ir.createdAt, ir.recordOrder AS item_order,
               ir.updatedAt, ir.syncedAt AS synced_at, ir.isDeleted AS is_deleted,
               0 AS hide_in_owner_inbox, ir.version
        FROM workspace_inbox_records ir
        LEFT JOIN workspaces w ON w.id = ir.workspaceId
        JOIN inbox_record_links irl ON irl.record_id = ir.id
        WHERE irl.context_id = :contextId
          AND ir.isDeleted = 0
        ORDER BY item_order
        """,
    )
    fun getRecordsForContextStream(contextId: String): kotlinx.coroutines.flow.Flow<List<InboxRecord>>

    @Query(
        """
        SELECT ir.id, COALESCE(w.sourceContextId, ir.workspaceId) AS contextId,
               ir.text, ir.createdAt, ir.recordOrder AS item_order,
               ir.updatedAt, ir.syncedAt AS synced_at, ir.isDeleted AS is_deleted,
               0 AS hide_in_owner_inbox, ir.version
        FROM workspace_inbox_records ir
        LEFT JOIN workspaces w ON w.id = ir.workspaceId
        JOIN workspace_capability_instances ci ON ci.id = ir.capabilityInstanceId
        WHERE ir.workspaceId = :contextId
          AND ir.isDeleted = 0
          AND (
            ci.configuration != '{"ownerVisibility":"HIDE_WHEN_ASSOCIATED"}'
            OR NOT EXISTS (
              SELECT 1
              FROM inbox_record_links owner_links
              WHERE owner_links.record_id = ir.id
            )
          )
        UNION
        SELECT ir.id, COALESCE(w.sourceContextId, ir.workspaceId) AS contextId,
               ir.text, ir.createdAt, ir.recordOrder AS item_order,
               ir.updatedAt, ir.syncedAt AS synced_at, ir.isDeleted AS is_deleted,
               0 AS hide_in_owner_inbox, ir.version
        FROM workspace_inbox_records ir
        LEFT JOIN workspaces w ON w.id = ir.workspaceId
        JOIN inbox_record_links irl ON irl.record_id = ir.id
        WHERE irl.context_id = :contextId
          AND ir.isDeleted = 0
        ORDER BY item_order
        """,
    )
    suspend fun getRecordsForContext(contextId: String): List<InboxRecord>
}
