package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask

data class CanonicalFocusSplitSourceVersion(
    val itemId: String,
    val expectedVersion: Long,
)

data class CanonicalTaskSplitSourceVersion(
    val taskId: String,
    val expectedVersion: Long,
)

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

    @Query(
        """
        SELECT * FROM day_focus_items
        WHERE recurrenceSeriesId = :seriesId
        ORDER BY dayPlanId ASC, `order` ASC, createdAt ASC
        """,
    )
    suspend fun getFocusOccurrencesForSeries(seriesId: String): List<DayFocusItem>

    @Update
    suspend fun updateFocusOccurrences(items: List<DayFocusItem>)

    @Transaction
    suspend fun updateSeriesAndFocusOccurrences(
        series: CanonicalRecurringSeriesEntity,
        occurrences: List<DayFocusItem>,
    ) {
        update(series)
        if (occurrences.isNotEmpty()) {
            updateFocusOccurrences(occurrences)
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFocusOccurrenceForAuthoring(item: DayFocusItem)

    @Query(
        """
        UPDATE day_focus_items
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :itemId
          AND version = :expectedVersion
          AND isDeleted = 0
          AND recurrenceSeriesId IS NULL
        """,
    )
    suspend fun softDeleteOneOffForConversion(
        itemId: String,
        expectedVersion: Long,
        updatedAt: Long,
    ): Int

    @Transaction
    suspend fun convertOneOffToCanonicalSeries(
        series: CanonicalRecurringSeriesEntity,
        occurrence: DayFocusItem,
        sourceItemId: String,
        sourceExpectedVersion: Long,
        updatedAt: Long,
    ) {
        insert(series)
        insertFocusOccurrenceForAuthoring(occurrence)

        val deletedCount =
            softDeleteOneOffForConversion(
                itemId = sourceItemId,
                expectedVersion = sourceExpectedVersion,
                updatedAt = updatedAt,
            )
        check(deletedCount == 1) {
            "Cannot convert stale, deleted, or already-recurring focus item: $sourceItemId"
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTaskOccurrenceForAuthoring(task: DayTask)

    @Query(
        """
        UPDATE day_tasks
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :taskId
          AND version = :expectedVersion
          AND isDeleted = 0
          AND recurrenceSeriesId IS NULL
          AND recurringTaskId IS NULL
        """,
    )
    suspend fun softDeleteTaskOneOffForConversion(
        taskId: String,
        expectedVersion: Long,
        updatedAt: Long,
    ): Int

    @Transaction
    suspend fun convertTaskOneOffToCanonicalSeries(
        series: CanonicalRecurringSeriesEntity,
        occurrence: DayTask,
        sourceTaskId: String,
        sourceExpectedVersion: Long,
        updatedAt: Long,
    ) {
        insert(series)
        insertTaskOccurrenceForAuthoring(occurrence)

        val deletedCount =
            softDeleteTaskOneOffForConversion(
                taskId = sourceTaskId,
                expectedVersion = sourceExpectedVersion,
                updatedAt = updatedAt,
            )
        check(deletedCount == 1) {
            "Cannot convert stale, deleted, legacy-recurring, or already-canonical task: $sourceTaskId"
        }
    }

    @Query(
        """
        SELECT * FROM day_tasks
        WHERE recurrenceSeriesId = :seriesId
        ORDER BY dayPlanId ASC, `order` ASC, createdAt ASC
        """,
    )
    suspend fun getTaskOccurrencesForSeries(seriesId: String): List<DayTask>

    @Update
    suspend fun updateTaskOccurrences(tasks: List<DayTask>)

    @Transaction
    suspend fun updateSeriesAndTaskOccurrences(
        series: CanonicalRecurringSeriesEntity,
        occurrences: List<DayTask>,
    ) {
        update(series)
        if (occurrences.isNotEmpty()) {
            updateTaskOccurrences(occurrences)
        }
    }

    @Query(
        """
        UPDATE canonical_recurring_series
        SET endDayKey = :endDayKey,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :seriesId
          AND version = :expectedVersion
          AND isDeleted = 0
        """,
    )
    suspend fun endCanonicalSeriesForSplit(
        seriesId: String,
        expectedVersion: Long,
        endDayKey: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE day_tasks
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :taskId
          AND version = :expectedVersion
          AND recurrenceSeriesId = :seriesId
          AND recurrenceOccurrenceDayKey IS NOT NULL
          AND recurringTaskId IS NULL
          AND isDeleted = 0
        """,
    )
    suspend fun softDeleteCanonicalTaskOccurrence(
        taskId: String,
        expectedVersion: Long,
        seriesId: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE day_tasks
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE recurrenceSeriesId = :seriesId
          AND recurrenceOccurrenceDayKey >= :fromDayKey
          AND recurringTaskId IS NULL
          AND isDeleted = 0
        """,
    )
    suspend fun softDeleteCanonicalTaskOccurrencesFromDay(
        seriesId: String,
        fromDayKey: String,
        updatedAt: Long,
    ): Int

    @Transaction
    suspend fun stopCanonicalTaskSeriesFromDay(
        seriesId: String,
        expectedSeriesVersion: Long,
        fromDayKey: String,
        endDayKey: String,
        updatedAt: Long,
    ) {
        val seriesUpdateCount =
            endCanonicalSeriesForSplit(
                seriesId = seriesId,
                expectedVersion = expectedSeriesVersion,
                endDayKey = endDayKey,
                updatedAt = updatedAt,
            )
        check(seriesUpdateCount == 1) {
            "Cannot stop stale or deleted canonical task series: $seriesId"
        }

        softDeleteCanonicalTaskOccurrencesFromDay(
            seriesId = seriesId,
            fromDayKey = fromDayKey,
            updatedAt = updatedAt,
        )
    }

    @Query(
        """
        UPDATE day_focus_items
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :itemId
          AND version = :expectedVersion
          AND recurrenceSeriesId = :seriesId
          AND isDeleted = 0
        """,
    )
    suspend fun softDeleteFocusOccurrenceForSplit(
        itemId: String,
        expectedVersion: Long,
        seriesId: String,
        updatedAt: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCanonicalSeriesForSplit(series: CanonicalRecurringSeriesEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFocusOccurrencesForSplit(items: List<DayFocusItem>)

    @Transaction
    suspend fun splitCanonicalFocusSeries(
        oldSeriesId: String,
        oldSeriesExpectedVersion: Long,
        oldSeriesEndDayKey: String,
        newSeries: CanonicalRecurringSeriesEntity,
        liveSourceOccurrences: List<CanonicalFocusSplitSourceVersion>,
        replacementOccurrences: List<DayFocusItem>,
        updatedAt: Long,
    ) {
        val seriesUpdateCount =
            endCanonicalSeriesForSplit(
                seriesId = oldSeriesId,
                expectedVersion = oldSeriesExpectedVersion,
                endDayKey = oldSeriesEndDayKey,
                updatedAt = updatedAt,
            )
        check(seriesUpdateCount == 1) {
            "Cannot split stale or deleted canonical series: $oldSeriesId"
        }

        insertCanonicalSeriesForSplit(newSeries)

        liveSourceOccurrences.forEach { source ->
            val deletedCount =
                softDeleteFocusOccurrenceForSplit(
                    itemId = source.itemId,
                    expectedVersion = source.expectedVersion,
                    seriesId = oldSeriesId,
                    updatedAt = updatedAt,
                )
            check(deletedCount == 1) {
                "Cannot split stale canonical focus occurrence: ${source.itemId}"
            }
        }

        if (replacementOccurrences.isNotEmpty()) {
            insertFocusOccurrencesForSplit(replacementOccurrences)
        }
    }

    @Query(
        """
        UPDATE day_tasks
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :taskId
          AND version = :expectedVersion
          AND recurrenceSeriesId = :seriesId
          AND recurringTaskId IS NULL
          AND isDeleted = 0
        """,
    )
    suspend fun softDeleteTaskOccurrenceForSplit(
        taskId: String,
        expectedVersion: Long,
        seriesId: String,
        updatedAt: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTaskOccurrencesForSplit(tasks: List<DayTask>)

    @Transaction
    suspend fun splitCanonicalTaskSeries(
        oldSeriesId: String,
        oldSeriesExpectedVersion: Long,
        oldSeriesEndDayKey: String,
        newSeries: CanonicalRecurringSeriesEntity,
        liveSourceOccurrences: List<CanonicalTaskSplitSourceVersion>,
        replacementOccurrences: List<DayTask>,
        updatedAt: Long,
    ) {
        val seriesUpdateCount =
            endCanonicalSeriesForSplit(
                seriesId = oldSeriesId,
                expectedVersion = oldSeriesExpectedVersion,
                endDayKey = oldSeriesEndDayKey,
                updatedAt = updatedAt,
            )
        check(seriesUpdateCount == 1) {
            "Cannot split stale or deleted canonical task series: $oldSeriesId"
        }

        insertCanonicalSeriesForSplit(newSeries)

        liveSourceOccurrences.forEach { source ->
            val deletedCount =
                softDeleteTaskOccurrenceForSplit(
                    taskId = source.taskId,
                    expectedVersion = source.expectedVersion,
                    seriesId = oldSeriesId,
                    updatedAt = updatedAt,
                )
            check(deletedCount == 1) {
                "Cannot split stale canonical task occurrence: ${source.taskId}"
            }
        }

        if (replacementOccurrences.isNotEmpty()) {
            insertTaskOccurrencesForSplit(replacementOccurrences)
        }
    }

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
