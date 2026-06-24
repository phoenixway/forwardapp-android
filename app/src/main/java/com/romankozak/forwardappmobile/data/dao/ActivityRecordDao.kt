package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActivityRecord)

    @Update
    suspend fun update(record: ActivityRecord)

    @Query("SELECT * FROM activity_records ORDER BY createdAt ASC")
    fun getAllRecordsStream(): Flow<List<ActivityRecord>>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE id IN (
            SELECT id FROM activity_records
            ORDER BY createdAt DESC
            LIMIT :limit
        )
        ORDER BY createdAt ASC
        """,
    )
    fun getRecentRecordsStream(limit: Int): Flow<List<ActivityRecord>>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE createdAt < :beforeCreatedAt
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getOlderRecordsBefore(
        beforeCreatedAt: Long,
        limit: Int,
    ): List<ActivityRecord>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE endTime IS NULL
          AND startTime IS NOT NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    suspend fun findLastOngoingActivity(): ActivityRecord?

    @Query(
        """
        SELECT * FROM activity_records
        WHERE goal_id = :goalId
          AND endTime IS NULL
          AND startTime IS NOT NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    suspend fun findLastOngoingActivityForGoal(goalId: String): ActivityRecord?

    @Query(
        """
        SELECT * FROM activity_records
        WHERE context_id = :contextId
          AND endTime IS NULL
          AND startTime IS NOT NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    suspend fun findLastOngoingActivityForContext(contextId: String): ActivityRecord?

    @Query(
        """
        SELECT * FROM activity_records
        WHERE (context_id = :contextId OR goal_id IN (:goalIds))
        AND createdAt BETWEEN :startTime AND :endTime
        AND startTime IS NOT NULL AND endTime IS NOT NULL
    """,
    )
    suspend fun getCompletedActivitiesForContext(
        contextId: String,
        goalIds: List<String>,
        startTime: Long,
        endTime: Long,
    ): List<ActivityRecord>

    @Query("DELETE FROM activity_records")
    suspend fun clearAll()

    @Delete
    suspend fun delete(record: ActivityRecord)

    @Query("DELETE FROM activity_records WHERE id = :recordId")
    suspend fun deleteById(recordId: String)

    @Query(
        """
        DELETE FROM activity_records
        WHERE id IN (
            SELECT id FROM activity_records
            WHERE target_type = :targetType
            ORDER BY createdAt DESC
            LIMIT -1 OFFSET :keepCount
        )
        """,
    )
    suspend fun deleteByTargetTypeKeepingNewest(
        targetType: String,
        keepCount: Int,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ActivityRecord>)

    @Query(
        """
        SELECT ar.* FROM activity_records AS ar
        JOIN activity_records_fts AS fts ON ar.id = fts.rowid
        WHERE fts.text MATCH :query
        ORDER BY ar.createdAt DESC
    """,
    )
    suspend fun search(query: String): List<ActivityRecord>

    @Query(
        """
    SELECT * FROM activity_records
    WHERE (context_id = :contextId OR goal_id IN (:goalIds))
    AND startTime IS NOT NULL AND endTime IS NOT NULL
""",
    )
    suspend fun getAllCompletedActivitiesForContext(
        contextId: String,
        goalIds: List<String>,
    ): List<ActivityRecord>

    @Query("SELECT * FROM activity_records WHERE id = :recordId")
    suspend fun findById(recordId: String): ActivityRecord?

    @Query(
        """
        SELECT * FROM activity_records
        WHERE record_kind = :recordKind
          AND target_type = :targetType
          AND target_id = :targetId
        ORDER BY updatedAt DESC, createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun findByKindAndTarget(
        recordKind: String,
        targetType: String,
        targetId: String,
    ): ActivityRecord?

    @Query(
        """
        SELECT * FROM activity_records
        WHERE createdAt >= :fromTimestamp
        ORDER BY COALESCE(startTime, createdAt) DESC
        """,
    )
    suspend fun getRecordsFrom(fromTimestamp: Long): List<ActivityRecord>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE createdAt BETWEEN :fromTimestamp AND :toTimestamp
        ORDER BY COALESCE(startTime, createdAt) DESC
        """,
    )
    suspend fun getRecordsBetween(
        fromTimestamp: Long,
        toTimestamp: Long,
    ): List<ActivityRecord>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE endTime IS NULL
          AND startTime IS NOT NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    fun findLastOngoingActivityFlow(): Flow<ActivityRecord?>

    @Query("SELECT * FROM activity_records WHERE context_id = :contextId ORDER BY createdAt DESC")
    fun getRecordsForContextStream(contextId: String): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records")
    suspend fun getAllRaw(): List<ActivityRecord>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE context_id IS NOT NULL
          AND startTime IS NOT NULL
          AND endTime IS NOT NULL
          AND startTime BETWEEN :fromTimestamp AND :toTimestamp
        ORDER BY startTime ASC
        """,
    )
    suspend fun getCompletedContextActivitiesBetween(
        fromTimestamp: Long,
        toTimestamp: Long,
    ): List<ActivityRecord>
}
