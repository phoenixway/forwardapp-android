package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity

@Dao
interface CanonicalRecurringSeriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: CanonicalRecurringSeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<CanonicalRecurringSeriesEntity>)

    @Update
    suspend fun update(series: CanonicalRecurringSeriesEntity)

    @Query("SELECT * FROM canonical_recurring_series WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CanonicalRecurringSeriesEntity?

    /** Includes tombstones because canonical recurrence state is replicated state. */
    @Query("SELECT * FROM canonical_recurring_series ORDER BY createdAt ASC, id ASC")
    suspend fun getAllSync(): List<CanonicalRecurringSeriesEntity>

    @Query(
        """
        SELECT * FROM canonical_recurring_series
        WHERE isDeleted = 0
          AND startDayKey <= :dayKey
          AND (endDayKey IS NULL OR endDayKey >= :dayKey)
        ORDER BY createdAt ASC, id ASC
        """,
    )
    suspend fun getActiveCandidatesForDay(dayKey: String): List<CanonicalRecurringSeriesEntity>
}
