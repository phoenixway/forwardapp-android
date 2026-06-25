package com.romankozak.forwardappmobile.features.mainscreen.arc

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArcQuestDao {
    @Query(
        """
        SELECT * FROM arc_quests
        WHERE arc_key = :arcKey AND is_deleted = 0
        ORDER BY quest_order ASC, createdAt ASC
        """,
    )
    fun observeArcQuests(arcKey: String): Flow<List<ArcQuestEntity>>

    @Query("SELECT * FROM arc_quests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ArcQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quest: ArcQuestEntity)

    @Update
    suspend fun update(quest: ArcQuestEntity)

    @Query("SELECT COALESCE(MIN(quest_order), 0) FROM arc_quests WHERE arc_key = :arcKey AND is_deleted = 0")
    suspend fun getMinOrder(arcKey: String): Long

    @Query(
        """
        UPDATE arc_quests
        SET quest_order = :order, updatedAt = :updatedAt, synced_at = NULL, version = version + 1
        WHERE id = :questId
        """,
    )
    suspend fun updateOrder(
        questId: String,
        order: Long,
        updatedAt: Long,
    )
}
