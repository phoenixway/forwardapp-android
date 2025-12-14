package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.AiEventEntity

@Dao
interface AiEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AiEventEntity)

    @Query("SELECT * FROM ai_events WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getEventsSince(since: Long): List<AiEventEntity>
}
