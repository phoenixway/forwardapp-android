package com.romankozak.forwardappmobile.features.missions.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalActivitySlot
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticalActivitySlotDao {
    @Query(
        """
        SELECT * FROM tactical_activity_slots
        WHERE is_deleted = 0
        ORDER BY slot_order ASC, created_at ASC
        """,
    )
    fun observeSlots(): Flow<List<TacticalActivitySlot>>

    @Query("SELECT * FROM tactical_activity_slots WHERE context_id = :contextId AND is_deleted = 0 LIMIT 1")
    suspend fun getActiveSlotForContext(contextId: String): TacticalActivitySlot?

    @Query("SELECT * FROM tactical_activity_slots WHERE context_id = :contextId LIMIT 1")
    suspend fun getSlotForContext(contextId: String): TacticalActivitySlot?

    @Query("SELECT COALESCE(MAX(slot_order), -1) FROM tactical_activity_slots WHERE is_deleted = 0")
    suspend fun getMaxSlotOrder(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TacticalActivitySlot)

    @Update
    suspend fun updateSlot(slot: TacticalActivitySlot)

    @Query(
        """
        UPDATE tactical_activity_slots
        SET is_deleted = 1, updated_at = :updatedAt, synced_at = NULL, version = version + 1
        WHERE context_id = :contextId AND is_deleted = 0
        """,
    )
    suspend fun softDeleteSlotByContextId(
        contextId: String,
        updatedAt: Long,
    )
}
