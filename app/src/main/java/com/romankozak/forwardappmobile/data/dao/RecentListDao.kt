package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.RecentListEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentListDao {
    @Upsert suspend fun logAccess(entry: RecentListEntry)

    @Query(
        """
        SELECT gl.* FROM goal_lists AS gl
        INNER JOIN recent_list_entries AS rle ON gl.id = rle.list_id
        ORDER BY rle.last_accessed DESC
        LIMIT :limit
    """,
    )
    fun getRecentLists(limit: Int): Flow<List<GoalList>>

    @Query("SELECT * FROM recent_list_entries")
    suspend fun getAllEntries(): List<RecentListEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RecentListEntry>)

    @Query("DELETE FROM recent_list_entries")
    suspend fun deleteAll()
}
