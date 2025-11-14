package com.romankozak.forwardappmobile.shared.features.activitytracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.activitytracker.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActivityRecordsRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ActivityRecordsRepository {

    override fun observeActivityRecords(): Flow<List<ActivityRecord>> =
        database.activityRecordsQueries.getActivityRecordsOrdered()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeActivityRecord(recordId: String): Flow<ActivityRecord?> =
        database.activityRecordsQueries.getActivityRecordById(recordId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.toDomain() }

    override fun searchActivityRecords(query: String): Flow<List<ActivityRecord>> =
        database.activityRecordsQueries.searchActivityRecords(query)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertActivityRecord(record: ActivityRecord) = withContext(dispatcher) {
        database.activityRecordsQueries.insertActivityRecord(
            id = record.id,
            name = record.name,
            description = record.description,
            createdAt = record.createdAt,
            startTime = record.startTime,
            endTime = record.endTime,
            totalTimeSpentMinutes = record.totalTimeSpentMinutes,
            tags = record.tags,
            relatedLinks = record.relatedLinks,
            isCompleted = record.isCompleted,
            activityType = record.activityType,
            parentProjectId = record.parentProjectId,
        )
    }

    override suspend fun deleteActivityRecord(recordId: String) = withContext(dispatcher) {
        database.activityRecordsQueries.deleteActivityRecord(recordId)
    }

    override suspend fun clearActivityRecords() = withContext(dispatcher) {
        database.activityRecordsQueries.deleteAllActivityRecords()
    }

    override suspend fun findLastOngoingActivity(): ActivityRecord? = withContext(dispatcher) {
        database.activityRecordsQueries.getLastOngoingActivity().executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findLastOngoingActivityForProject(projectId: String): ActivityRecord? = withContext(dispatcher) {
        database.activityRecordsQueries.getLastOngoingActivityForProject(projectId).executeAsOneOrNull()?.toDomain()
    }
}
