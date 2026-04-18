package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTagLookup
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTagRef

@Dao
interface ContextTagRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ContextTagRef>)

    @Query("DELETE FROM context_tag_refs WHERE context_id = :contextId")
    suspend fun deleteForContext(contextId: String)

    @Query(
        """
        SELECT ctr.context_id, ctr.normalized_tag
        FROM context_tag_refs ctr
        JOIN contexts c ON c.id = ctr.context_id
        WHERE ctr.normalized_tag IN (:normalizedTags)
          AND c.is_deleted = 0
        ORDER BY c.goal_order ASC, c.createdAt ASC
        """,
    )
    suspend fun findContextsByTags(normalizedTags: List<String>): List<ContextTagLookup>

    @Query(
        """
        SELECT ctr.context_id
        FROM context_tag_refs ctr
        JOIN contexts c ON c.id = ctr.context_id
        WHERE ctr.normalized_tag = :normalizedTag
          AND c.is_deleted = 0
        ORDER BY c.goal_order ASC, c.createdAt ASC
        """,
    )
    suspend fun findContextIdsByTag(normalizedTag: String): List<String>
}
