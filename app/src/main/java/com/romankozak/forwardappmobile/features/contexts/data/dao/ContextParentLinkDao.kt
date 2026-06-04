package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextParentLinkDao {
    @Query(
        """
        SELECT *
        FROM context_parent_links
        WHERE is_deleted = 0
        ORDER BY parent_context_id ASC, link_order ASC
        """,
    )
    fun observeActiveLinks(): Flow<List<ContextParentLink>>

    @Query("SELECT * FROM context_parent_links WHERE is_deleted = 0")
    suspend fun getActiveLinks(): List<ContextParentLink>

    @Query("SELECT * FROM context_parent_links")
    suspend fun getAllRaw(): List<ContextParentLink>

    @Query(
        """
        SELECT COALESCE(MAX(link_order), -1)
        FROM context_parent_links
        WHERE parent_context_id = :parentContextId
        """,
    )
    suspend fun getMaxOrderForParent(parentContextId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<ContextParentLink>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: ContextParentLink)

    @Query(
        """
        UPDATE context_parent_links
        SET is_deleted = 1,
            updatedAt = :updatedAt,
            version = version + 1
        WHERE parent_context_id = :parentContextId
            AND child_context_id = :childContextId
        """,
    )
    suspend fun softDelete(
        parentContextId: String,
        childContextId: String,
        updatedAt: Long = System.currentTimeMillis(),
    )
}
