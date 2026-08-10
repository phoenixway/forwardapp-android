package com.romankozak.forwardappmobile.features.missions.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticalIterationDao {
    @Query(
        """
        SELECT * FROM tactical_iterations
        WHERE is_deleted = 0
        ORDER BY started_at DESC, created_at DESC
        """,
    )
    fun observeIterations(): Flow<List<TacticalIteration>>

    @Query(
        """
        SELECT * FROM tactical_iterations
        WHERE status IN ('DRAFT', 'ACTIVE') AND is_deleted = 0
        ORDER BY started_at DESC, created_at DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveIteration(): TacticalIteration?

    @Query("SELECT * FROM tactical_iterations WHERE id = :iterationId LIMIT 1")
    suspend fun getById(iterationId: String): TacticalIteration?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(iteration: TacticalIteration)

    @Update
    suspend fun update(iteration: TacticalIteration)

    @Query(
        """
        UPDATE tactical_iterations
        SET status = 'ACTIVE',
            started_at = :startedAt,
            updated_at = :startedAt,
            synced_at = NULL,
            version = version + 1
        WHERE id = :iterationId
        """,
    )
    suspend fun startIteration(
        iterationId: String,
        startedAt: Long,
    )

    @Query(
        """
        UPDATE tactical_iterations
        SET status = 'CLOSED',
            closed_at = :closedAt,
            updated_at = :closedAt,
            synced_at = NULL,
            version = version + 1
        WHERE id = :iterationId
        """,
    )
    suspend fun closeIteration(
        iterationId: String,
        closedAt: Long,
    )
}
