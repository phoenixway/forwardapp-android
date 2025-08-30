// --- File: com/romankozak/forwardappmobile/data/dao/ListItemDao.kt ---
package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.ListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
// ListItemDao.kt

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY item_order ASC, id ASC")
    fun getItemsForListStream(listId: String): Flow<List<ListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ListItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ListItem>)

    @Update
    suspend fun updateItem(item: ListItem)

    @Update
    suspend fun updateItems(items: List<ListItem>)

    @Query("DELETE FROM list_items WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<String>)

    @Query("DELETE FROM list_items WHERE listId IN (:listIds)")
    suspend fun deleteItemsForLists(listIds: List<String>)

    @Query("SELECT * FROM list_items")
    suspend fun getAll(): List<ListItem>

    // ... існуючі методи в ListItemDao

    @Query("SELECT COUNT(*) FROM list_items WHERE entityId = :entityId AND listId = :listId")
    suspend fun getLinkCount(entityId: String, listId: String): Int

    @Query("DELETE FROM list_items WHERE entityId = :entityId AND listId = :listId")
    suspend fun deleteLinkByEntityAndList(entityId: String, listId: String)

    @Query("UPDATE list_items SET listId = :targetListId WHERE id IN (:itemIds)")
    suspend fun updateListItemListIds(itemIds: List<String>, targetListId: String)

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY item_order ASC, id ASC")
    suspend fun getItemsForListSyncForDebug(listId: String): List<ListItem>

}