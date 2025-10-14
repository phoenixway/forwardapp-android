package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.CustomListEntity
import com.romankozak.forwardappmobile.data.database.models.CustomListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomList(customList: CustomListEntity)

    @Update
    suspend fun updateCustomList(customList: CustomListEntity)

    @Query("SELECT * FROM custom_lists WHERE id = :listId")
    suspend fun getCustomListById(listId: String): CustomListEntity?

    @Query("SELECT * FROM custom_lists WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getCustomListsForProject(projectId: String): Flow<List<CustomListEntity>>

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteCustomListById(listId: String)

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItem(item: CustomListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItems(items: List<CustomListItemEntity>)

    @Update
    suspend fun updateListItem(item: CustomListItemEntity)

    @Update
    suspend fun updateListItems(items: List<CustomListItemEntity>)

    @Query("SELECT * FROM custom_list_items WHERE id = :itemId")
    suspend fun getListItemById(itemId: String): CustomListItemEntity?

    @Query("SELECT * FROM custom_list_items WHERE listId = :listId ORDER BY itemOrder ASC")
    fun getListItemsForList(listId: String): Flow<List<CustomListItemEntity>>

    @Query("DELETE FROM custom_list_items WHERE id = :itemId")
    suspend fun deleteListItemById(itemId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomLists(lists: List<CustomListEntity>)

    @Query("DELETE FROM custom_lists")
    suspend fun deleteAllCustomLists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllListItems(items: List<CustomListItemEntity>)

    @Query("DELETE FROM custom_list_items")
    suspend fun deleteAllListItems()

    @Query("SELECT * FROM custom_lists")
    suspend fun getAllCustomLists(): List<CustomListEntity>

    @Query("SELECT * FROM custom_lists")
    fun getAllCustomListsAsFlow(): Flow<List<CustomListEntity>>

    @Query("SELECT * FROM custom_list_items")
    suspend fun getAllListItems(): List<CustomListItemEntity>

    @Query("DELETE FROM custom_list_items WHERE id IN (:itemIds)")
    suspend fun deleteListItemsByIds(itemIds: List<String>)
}
