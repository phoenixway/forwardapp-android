package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.database.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem

@Dao
interface LinkItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(linkItem: LinkItemEntity)

    @Query("SELECT * FROM link_items WHERE id = :id")
    suspend fun getLinkItemById(id: String): LinkItemEntity?

    @Query("SELECT * FROM list_items")
    suspend fun getAll(): List<ListItem>

    @Transaction
    @Query(
        """
    SELECT li.*, l.project_id as projectId, p.name as projectName, l.id as listItemId
    FROM link_items li
    INNER JOIN list_items l ON li.id = l.entityId
    INNER JOIN projects p ON l.project_id = p.id
    WHERE l.itemType = 'LINK_ITEM' AND li.link_data LIKE :query
""",
    )
    suspend fun searchLinksGlobal(query: String): List<GlobalLinkSearchResult>

    @Query("SELECT * FROM link_items")
    suspend fun getAllEntities(): List<LinkItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(linkItems: List<LinkItemEntity>)

    @Query("DELETE FROM link_items")
    suspend fun deleteAll()
}