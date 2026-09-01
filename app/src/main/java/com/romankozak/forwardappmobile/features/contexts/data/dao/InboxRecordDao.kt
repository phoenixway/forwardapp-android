package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import kotlinx.coroutines.flow.Flow

/** Read-only compatibility projection over canonical Workspace Inbox records. */
@Dao
interface InboxRecordDao {
    @Query("${SELECT_COMPATIBILITY_RECORDS} WHERE r.workspaceId = :contextId AND r.isDeleted = 0 ORDER BY r.recordOrder")
    fun getOwnedRecordsForContextStream(contextId: String): Flow<List<InboxRecord>>

    @Query("${SELECT_COMPATIBILITY_RECORDS} WHERE r.workspaceId = :contextId AND r.isDeleted = 0 ORDER BY r.recordOrder")
    suspend fun getOwnedRecordsForContext(contextId: String): List<InboxRecord>

    @Query("${SELECT_COMPATIBILITY_RECORDS} WHERE r.id = :id LIMIT 1")
    suspend fun getRecordById(id: String): InboxRecord?

    @Query("${SELECT_COMPATIBILITY_RECORDS} WHERE r.text LIKE :query ORDER BY r.createdAt DESC")
    suspend fun searchInboxRecordsGlobal(query: String): List<InboxRecord>

    @Query("$SELECT_COMPATIBILITY_RECORDS ORDER BY r.recordOrder")
    suspend fun getAll(): List<InboxRecord>

    @Query("$SELECT_COMPATIBILITY_RECORDS ORDER BY r.recordOrder")
    suspend fun getAllRaw(): List<InboxRecord>

    private companion object {
        const val SELECT_COMPATIBILITY_RECORDS =
            """
            SELECT r.id AS id,
                   COALESCE(w.sourceContextId, r.workspaceId) AS contextId,
                   r.text AS text,
                   r.createdAt AS createdAt,
                   r.recordOrder AS item_order,
                   r.updatedAt AS updatedAt,
                   r.syncedAt AS synced_at,
                   r.isDeleted AS is_deleted,
                   0 AS hide_in_owner_inbox,
                   r.version AS version
            FROM workspace_inbox_records r
            LEFT JOIN workspaces w ON w.id = r.workspaceId
            """
    }
}
