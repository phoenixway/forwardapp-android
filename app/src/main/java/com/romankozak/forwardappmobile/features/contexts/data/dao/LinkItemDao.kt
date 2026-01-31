package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(linkItem: LinkItemEntity)

    @Query("SELECT * FROM link_items WHERE id = :id")
    suspend fun getLinkItemById(id: String): LinkItemEntity?

    @Query("SELECT * FROM list_items")
    suspend fun getAll(): List<BacklogItem>

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
        l.context_id as contextId, 
        p.name as contextName, 
        l.id as listItemId,
        pc.path as pathSegments
    FROM link_items li
    INNER JOIN list_items l ON li.id = l.entityId
    INNER JOIN contexts p ON l.context_id = p.id
    INNER JOIN path_cte pc ON p.id = pc.id
    WHERE l.itemType = 'LINK_ITEM' AND li.link_data LIKE :query
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
