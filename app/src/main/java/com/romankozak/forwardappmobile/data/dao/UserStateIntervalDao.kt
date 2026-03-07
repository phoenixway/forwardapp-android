package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.UserStateIntervalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStateIntervalDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(interval: UserStateIntervalEntity): Long

    @Query(
        """
        SELECT * FROM user_state_intervals
        WHERE endedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getActive(): UserStateIntervalEntity?

    @Query(
        """
        SELECT * FROM user_state_intervals
        WHERE endedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    fun observeActive(): Flow<UserStateIntervalEntity?>

    @Query("UPDATE user_state_intervals SET endedAt = :endedAt WHERE endedAt IS NULL")
    suspend fun closeActiveIntervals(endedAt: Long): Int

    @Query(
        """
        SELECT * FROM user_state_intervals
        WHERE startedAt < :toInclusive AND (endedAt IS NULL OR endedAt > :fromInclusive)
        ORDER BY startedAt ASC
        """,
    )
    suspend fun getTimeline(
        fromInclusive: Long,
        toInclusive: Long,
    ): List<UserStateIntervalEntity>

    @Query(
        """
        SELECT * FROM user_state_intervals
        WHERE startedAt <= :atMillis
          AND (endedAt IS NULL OR endedAt > :atMillis)
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveAt(atMillis: Long): UserStateIntervalEntity?

    @Query("SELECT * FROM user_state_intervals")
    suspend fun getAllRaw(): List<UserStateIntervalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(intervals: List<UserStateIntervalEntity>)

    @Query("DELETE FROM user_state_intervals")
    suspend fun deleteAll()
}
