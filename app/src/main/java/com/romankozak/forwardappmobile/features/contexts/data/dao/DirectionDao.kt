package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectionDao {
    @Query("SELECT * FROM direction_items WHERE contextId = :contextId AND is_deleted = 0 ORDER BY itemOrder ASC")
    fun getDirectionItemsForContext(contextId: String): Flow<List<DirectionItemEntity>>

    @Query("SELECT * FROM direction_items")
    suspend fun getAllRaw(): List<DirectionItemEntity>

    @Query("SELECT * FROM direction_items WHERE id = :itemId LIMIT 1")
    suspend fun getById(itemId: String): DirectionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DirectionItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DirectionItemEntity>)

    @Update
    suspend fun update(item: DirectionItemEntity)

    @Update
    suspend fun updateAll(items: List<DirectionItemEntity>)
    
    @Query("UPDATE direction_items SET is_deleted = 1, updatedAt = :updatedAt, version = :version WHERE id = :itemId")
    suspend fun markDeleted(
        itemId: String,
        updatedAt: Long,
        version: Long,
    )

    @Query("SELECT COUNT(id) FROM direction_items WHERE contextId = :contextId AND is_deleted = 0")
    suspend fun count(contextId: String): Int

    @Query("DELETE FROM direction_items")
    suspend fun deleteAll()
}
