package com.romankozak.forwardappmobile.shared.features.activity_records.data

import com.romankozak.forwardappmobile.shared.data.database.models.ActivityRecord
import kotlinx.coroutines.flow.Flow

interface ActivityRecordRepository {
    suspend fun insert(record: ActivityRecord)

    suspend fun update(record: ActivityRecord)

    fun getAllRecordsStream(): Flow<List<ActivityRecord>>

    suspend fun findLastOngoingActivity(): ActivityRecord?

    suspend fun findLastOngoingActivityForGoal(goalId: String): ActivityRecord?

    suspend fun findLastOngoingActivityForProject(projectId: String): ActivityRecord?

    suspend fun getCompletedActivitiesForProject(
        projectId: String,
        goalIds: List<String>,
        startTime: Long,
        endTime: Long,
    ): List<ActivityRecord>

    suspend fun clearAll()

    suspend fun delete(record: ActivityRecord)

    suspend fun insertAll(records: List<ActivityRecord>)

    suspend fun search(query: String): List<ActivityRecord>

    suspend fun getAllCompletedActivitiesForProject(
        projectId: String,
        goalIds: List<String>,
    ): List<ActivityRecord>

    suspend fun findById(recordId: String): ActivityRecord?
}
