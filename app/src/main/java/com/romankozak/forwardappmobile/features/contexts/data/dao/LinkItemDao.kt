package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(linkItem: LinkItemEntity)

    @Query("SELECT * FROM link_items WHERE id = :id")
    suspend fun getLinkItemById(id: String): LinkItemEntity?

    @Transaction
    @Query(
        """
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM contexts WHERE parentId IS NULL
        UNION ALL
        SELECT p.id, p.name, pct.path || ' / ' || p.name
        FROM contexts p JOIN path_cte pct ON p.parentId = pct.id
    )
    SELECT
        li.*,
        entry.workspaceId as contextId,
        p.name as contextName,
        entry.id as listItemId,
        pc.path as pathSegments
    FROM link_items li
    INNER JOIN workspace_backlog_entries entry
      ON li.id = entry.targetId
     AND entry.targetKind = 'LINK_ITEM'
     AND entry.isDeleted = 0
    INNER JOIN contexts p ON entry.workspaceId = p.id
    INNER JOIN path_cte pc ON p.id = pc.id
    WHERE li.link_data LIKE :query
      AND li.is_deleted = 0
""",
    )
    suspend fun searchLinksGlobal(query: String): List<GlobalLinkSearchResult>

    @Query("SELECT * FROM link_items")
    suspend fun getAllEntities(): List<LinkItemEntity>

    @Query("SELECT * FROM link_items")
    fun getAllEntitiesAsFlow(): Flow<List<LinkItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LinkItemEntity>)

    @Query("DELETE FROM link_items")
    suspend fun deleteAll()

    @Query("DELETE FROM link_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM link_items")
    suspend fun getAllRaw(): List<LinkItemEntity>
}
