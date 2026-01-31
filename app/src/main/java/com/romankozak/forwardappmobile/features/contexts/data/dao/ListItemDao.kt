package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Query("SELECT * FROM list_items WHERE context_id = :contextId AND is_deleted = 0 ORDER BY item_order ASC, id ASC")
    fun getItemsForContextStream(contextId: String): Flow<List<BacklogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BacklogItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<BacklogItem>)

    @Update suspend fun updateItem(item: BacklogItem)

    @Update suspend fun updateItems(items: List<BacklogItem>)

    @Query("DELETE FROM list_items WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<String>)

    @Query("DELETE FROM list_items WHERE context_id IN (:contextIds)")
    suspend fun deleteItemsForContexts(contextIds: List<String>)

    @Query("SELECT * FROM list_items")
    suspend fun getAll(): List<BacklogItem>

    @Query("SELECT COUNT(*) FROM list_items WHERE entityId = :entityId AND context_id = :contextId")
    suspend fun getLinkCount(
        entityId: String,
        contextId: String,
    ): Int

    @Query("DELETE FROM list_items WHERE entityId = :entityId AND context_id = :contextId")
    suspend fun deleteLinkByEntityAndContext(
        entityId: String,
        contextId: String,
    )

    @Query("UPDATE list_items SET context_id = :targetContextId WHERE id IN (:itemIds)")
    suspend fun updateListItemContextIds(
        itemIds: List<String>,
        targetContextId: String,
    )

    @Query("SELECT * FROM list_items WHERE context_id = :contextId AND is_deleted = 0 ORDER BY item_order ASC, id ASC")
    suspend fun getItemsForContextSyncForDebug(contextId: String): List<BacklogItem>

    @Query("DELETE FROM list_items")
    suspend fun deleteAll()

    @Query("SELECT entityId FROM list_items WHERE context_id = :contextId AND itemType = 'GOAL'")
    suspend fun getGoalIdsForContext(contextId: String): List<String>

    @Query("DELETE FROM list_items WHERE entityId = :entityId")
    suspend fun deleteItemByEntityId(entityId: String)

    @Query("SELECT * FROM list_items WHERE entityId = :entityId LIMIT 1")
    suspend fun getListItemByEntityId(entityId: String): BacklogItem?

    @Query("SELECT * FROM list_items WHERE id IN (:ids)")
    suspend fun getItemsByIds(ids: List<String>): List<BacklogItem>

    /**
     * Знаходить ID проєкту, до якого належить певна сутність (наприклад, ціль).
     *
     * @param goalId ID сутності, для якої шукаємо проєкт.
     * @return ID проєкту або null, якщо не знайдено.
     */
    @Query("SELECT context_id FROM list_items WHERE entityId = :goalId LIMIT 1")
    suspend fun findContextIdForGoal(goalId: String): String?

    @Query("SELECT * FROM list_items")
    suspend fun getAllRaw(): List<BacklogItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BacklogItem>)
}
