package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.AiInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {
    @Query("SELECT * FROM ai_insights ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AiInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AiInsightEntity>)

    @Query("DELETE FROM ai_insights WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_insights")
    suspend fun clearAll()

    @Query("UPDATE ai_insights SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)
}
