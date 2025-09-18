package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActivityRecord)

    @Update
    suspend fun update(record: ActivityRecord)

    @Query("SELECT * FROM activity_records ORDER BY createdAt ASC")
    fun getAllRecordsStream(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records WHERE endTime IS NULL AND startTime IS NOT NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun findLastOngoingActivity(): ActivityRecord?

    @Query(
        "SELECT * FROM activity_records WHERE goal_id = :goalId AND endTime IS NULL AND startTime IS NOT NULL ORDER BY startTime DESC LIMIT 1",
    )
    suspend fun findLastOngoingActivityForGoal(goalId: String): ActivityRecord?

    @Query(
        "SELECT * FROM activity_records WHERE project_id = :projectId AND endTime IS NULL AND startTime IS NOT NULL ORDER BY startTime DESC LIMIT 1",
    )
    suspend fun findLastOngoingActivityForProject(projectId: String): ActivityRecord?

    @Query(
        """
        SELECT * FROM activity_records
        WHERE (project_id = :projectId OR goal_id IN (:goalIds))
        AND createdAt BETWEEN :startTime AND :endTime
        AND startTime IS NOT NULL AND endTime IS NOT NULL
    """,
    )
    suspend fun getCompletedActivitiesForProject(
        projectId: String,
        goalIds: List<String>,
        startTime: Long,
        endTime: Long,
    ): List<ActivityRecord>

    @Query("DELETE FROM activity_records")
    suspend fun clearAll()

    @Delete
    suspend fun delete(record: ActivityRecord)

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
    WHERE (project_id = :projectId OR goal_id IN (:goalIds))
    AND startTime IS NOT NULL AND endTime IS NOT NULL
""",
    )
    suspend fun getAllCompletedActivitiesForProject(
        projectId: String,
        goalIds: List<String>,
    ): List<ActivityRecord>

    @Query("SELECT * FROM activity_records WHERE id = :recordId")
    suspend fun findById(recordId: String): ActivityRecord?

    /* @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(records: List<ActivityRecord>)*/

}