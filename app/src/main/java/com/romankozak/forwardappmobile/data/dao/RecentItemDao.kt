package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logAccess(item: RecentItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecentItem>)

    @Query("SELECT * FROM recent_items ORDER BY lastAccessed DESC, id ASC LIMIT :limit")
    fun getRecentItems(limit: Int): Flow<List<RecentItem>>

    @Query("SELECT * FROM recent_items WHERE id = :id")
    suspend fun getRecentItemById(id: String): RecentItem?

    @Query("SELECT * FROM recent_items WHERE target = :contextId AND type = 'PROJECT' ORDER BY lastAccessed DESC, id ASC")
    fun getRecentItemsForContext(contextId: String): Flow<List<RecentItem>>

    @Query("SELECT * FROM recent_items")
    fun getAll(): List<RecentItem>

    @Query("DELETE FROM recent_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM recent_items")
    suspend fun getAllSync(): List<RecentItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSync(items: List<RecentItem>)

    @Query("DELETE FROM recent_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
