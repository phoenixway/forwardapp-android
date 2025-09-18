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
    @Query("SELECT * FROM list_items WHERE project_id = :projectId ORDER BY item_order ASC, id ASC")
    fun getItemsForProjectStream(projectId: String): Flow<List<ListItem>>

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

    @Query("DELETE FROM list_items WHERE project_id IN (:projectIds)")
    suspend fun deleteItemsForProjects(projectIds: List<String>)

    @Query("SELECT * FROM list_items")
    suspend fun getAll(): List<ListItem>

    @Query("SELECT COUNT(*) FROM list_items WHERE entityId = :entityId AND project_id = :projectId")
    suspend fun getLinkCount(
        entityId: String,
        projectId: String,
    ): Int

    @Query("DELETE FROM list_items WHERE entityId = :entityId AND project_id = :projectId")
    suspend fun deleteLinkByEntityAndProject(
        entityId: String,
        projectId: String,
    )

    @Query("UPDATE list_items SET project_id = :targetProjectId WHERE id IN (:itemIds)")
    suspend fun updateListItemProjectIds(
        itemIds: List<String>,
        targetProjectId: String,
    )

    @Query("SELECT * FROM list_items WHERE project_id = :projectId ORDER BY item_order ASC, id ASC")
    suspend fun getItemsForProjectSyncForDebug(projectId: String): List<ListItem>

    @Query("DELETE FROM list_items")
    suspend fun deleteAll()

    @Query("SELECT entityId FROM list_items WHERE project_id = :projectId AND itemType = 'GOAL'")
    suspend fun getGoalIdsForProject(projectId: String): List<String>
}