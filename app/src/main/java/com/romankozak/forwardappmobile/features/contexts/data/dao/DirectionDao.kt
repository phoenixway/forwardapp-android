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
    @Query("SELECT * FROM direction_items WHERE contextId = :contextId ORDER BY itemOrder ASC")
    fun getDirectionItemsForContext(contextId: String): Flow<List<DirectionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DirectionItemEntity)

    @Update
    suspend fun update(item: DirectionItemEntity)

    @Update
    suspend fun updateAll(items: List<DirectionItemEntity>)
    
    @Query("DELETE FROM direction_items WHERE id = :itemId")
    suspend fun delete(itemId: String)

    @Query("SELECT COUNT(id) FROM direction_items WHERE contextId = :contextId")
    suspend fun count(contextId: String): Int
}
