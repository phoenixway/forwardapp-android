package com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository

import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord
import kotlinx.coroutines.flow.Flow

interface ActivityRecordsRepository {
    fun observeActivityRecords(): Flow<List<ActivityRecord>>

    fun observeActivityRecord(recordId: String): Flow<ActivityRecord?>

    fun searchActivityRecords(query: String): Flow<List<ActivityRecord>>

    suspend fun upsertActivityRecord(record: ActivityRecord)

    suspend fun deleteActivityRecord(recordId: String)

    suspend fun clearActivityRecords()

    suspend fun findLastOngoingActivity(): ActivityRecord?

    suspend fun findLastOngoingActivityForProject(projectId: String): ActivityRecord?
}
