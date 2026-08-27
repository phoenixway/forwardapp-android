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
        SELECT ir.*
        FROM inbox_records ir
        WHERE ir.contextId = :contextId
          AND ir.is_deleted = 0
          AND (
            COALESCE(
              (
                SELECT cs.remove_inbox_entry_after_tag_autocopy
                FROM context_structures cs
                WHERE cs.contextId = ir.contextId
                  AND cs.isDeleted = 0
                LIMIT 1
              ),
              0
            ) = 0
            OR NOT EXISTS (
              SELECT 1
              FROM inbox_record_links owner_links
              WHERE owner_links.record_id = ir.id
            )
          )
        UNION
        SELECT ir.*
        FROM inbox_records ir
        JOIN inbox_record_links irl ON irl.record_id = ir.id
        WHERE irl.context_id = :contextId
          AND ir.is_deleted = 0
        ORDER BY item_order DESC
        """,
    )
    fun getRecordsForContextStream(contextId: String): kotlinx.coroutines.flow.Flow<List<InboxRecord>>

    @Query(
        """
        SELECT ir.*
        FROM inbox_records ir
        WHERE ir.contextId = :contextId
          AND ir.is_deleted = 0
          AND (
            COALESCE(
              (
                SELECT cs.remove_inbox_entry_after_tag_autocopy
                FROM context_structures cs
                WHERE cs.contextId = ir.contextId
                  AND cs.isDeleted = 0
                LIMIT 1
              ),
              0
            ) = 0
            OR NOT EXISTS (
              SELECT 1
              FROM inbox_record_links owner_links
              WHERE owner_links.record_id = ir.id
            )
          )
        UNION
        SELECT ir.*
        FROM inbox_records ir
        JOIN inbox_record_links irl ON irl.record_id = ir.id
        WHERE irl.context_id = :contextId
          AND ir.is_deleted = 0
        ORDER BY item_order DESC
        """,
    )
    suspend fun getRecordsForContext(contextId: String): List<InboxRecord>
}
