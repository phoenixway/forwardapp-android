package com.romankozak.forwardappmobile.features.missions.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionStreamDao {
    @Query(
        """
        SELECT * FROM mission_streams
        WHERE is_deleted = 0 AND is_archived = 0
        ORDER BY is_default DESC, stream_order ASC, created_at ASC
        """,
    )
    fun observeActiveStreams(): Flow<List<MissionStream>>

    @Query("SELECT * FROM mission_streams WHERE id = :streamId LIMIT 1")
    suspend fun getById(streamId: String): MissionStream?

    @Query("SELECT COALESCE(MAX(stream_order), -1) FROM mission_streams WHERE is_deleted = 0")
    suspend fun getMaxOrder(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stream: MissionStream)

    @Update
    suspend fun update(stream: MissionStream)

    @Query(
        """
        UPDATE mission_streams
        SET stream_order = :order, updated_at = :updatedAt, synced_at = NULL, version = version + 1
        WHERE id = :streamId
        """,
    )
    suspend fun updateOrder(
        streamId: String,
        order: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE mission_streams
        SET title = :title,
            description = :description,
            budget_percent = :budgetPercent,
            updated_at = :updatedAt,
            synced_at = NULL,
            version = version + 1
        WHERE id = :streamId
        """,
    )
    suspend fun updateDetails(
        streamId: String,
        title: String,
        description: String?,
        budgetPercent: Int?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE mission_streams
        SET is_archived = 1, updated_at = :updatedAt, synced_at = NULL, version = version + 1
        WHERE id = :streamId AND is_default = 0
        """,
    )
    suspend fun archiveNonDefault(
        streamId: String,
        updatedAt: Long,
    )
}
