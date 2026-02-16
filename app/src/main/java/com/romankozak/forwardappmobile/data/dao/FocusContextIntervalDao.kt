package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.FocusContextIntervalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusContextIntervalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interval: FocusContextIntervalEntity): Long

    @Query(
        """
        SELECT * FROM focus_context_intervals
        WHERE scope = :scope AND endedAt IS NULL
        ORDER BY COALESCE(priority, 0) DESC, startedAt DESC
        """,
    )
    fun observeActive(scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL): Flow<List<FocusContextIntervalEntity>>

    @Query(
        """
        SELECT * FROM focus_context_intervals
        WHERE contextId = :contextId AND scope = :scope AND endedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveByContext(
        contextId: String,
        scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL,
    ): FocusContextIntervalEntity?

    @Query(
        """
        UPDATE focus_context_intervals
        SET endedAt = :endedAt
        WHERE contextId = :contextId AND scope = :scope AND endedAt IS NULL
        """,
    )
    suspend fun closeActiveByContext(
        contextId: String,
        endedAt: Long,
        scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL,
    ): Int

    @Query(
        """
        DELETE FROM focus_context_intervals
        WHERE id IN (
            SELECT id FROM focus_context_intervals
            WHERE contextId = :contextId AND scope = :scope AND endedAt IS NULL
            ORDER BY startedAt DESC
            LIMIT -1 OFFSET 1
        )
        """,
    )
    suspend fun removeDuplicateActiveIntervals(
        contextId: String,
        scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL,
    )
}
