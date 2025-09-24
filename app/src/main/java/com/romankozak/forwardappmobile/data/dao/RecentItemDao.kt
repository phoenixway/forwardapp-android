package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logAccess(item: RecentItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecentItem>)

    @Query("SELECT * FROM recent_items ORDER BY lastAccessed DESC LIMIT :limit")
    fun getRecentItems(limit: Int): Flow<List<RecentItem>>

    @Query("DELETE FROM recent_items")
    suspend fun deleteAll()
}
