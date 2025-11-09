package com.romankozak.forwardappmobile.shared.features.activity_records.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.ActivityRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActivityRecordRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ActivityRecordRepository {

    override suspend fun insert(record: ActivityRecord) {
        withContext(ioDispatcher) {
            db.activityRecordsQueries.insert(record.toSqlDelight())
        }
    }

    override suspend fun update(record: ActivityRecord) {
        withContext(ioDispatcher) {
            db.activityRecordsQueries.update(record.toSqlDelight())
        }
    }

    override fun getAllRecordsStream(): Flow<List<ActivityRecord>> {
        return db.activityRecordsQueries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records -> records.map { it.toDomain() } }
    }

    override suspend fun findLastOngoingActivity(): ActivityRecord? {
        return withContext(ioDispatcher) {
            db.activityRecordsQueries.findLastOngoingActivity().executeAsOneOrNull()?.toDomain()
        }
    }

    // override suspend fun findLastOngoingActivityForGoal(goalId: String): ActivityRecord? {
    //     return withContext(ioDispatcher) {
    //         db.activityRecordsQueries.findLastOngoingActivityForGoal(goalId).executeAsOneOrNull()?.toDomain()
    //     }
    // }

    override suspend fun findLastOngoingActivityForProject(projectId: String): ActivityRecord? {
        return withContext(ioDispatcher) {
            db.activityRecordsQueries.findLastOngoingActivityForProject(projectId).executeAsOneOrNull()?.toDomain()
        }
    }

    // override suspend fun getCompletedActivitiesForProject(
    //     projectId: String,
    //     goalIds: List<String>,
    //     startTime: Long,
    //     endTime: Long,
    // ): List<ActivityRecord> {
    //     return withContext(ioDispatcher) {
    //         db.activityRecordsQueries.getCompletedActivitiesForProject(projectId, goalIds, startTime, endTime)
    //             .executeAsList()
    //             .map { it.toDomain() }
    //     }
    // }

    override suspend fun clearAll() {
        withContext(ioDispatcher) {
            db.activityRecordsQueries.clearAll()
        }
    }

    override suspend fun delete(record: ActivityRecord) {
        withContext(ioDispatcher) {
            db.activityRecordsQueries.delete(record.id)
        }
    }

    override suspend fun insertAll(records: List<ActivityRecord>) {
        withContext(ioDispatcher) {
            records.forEach { record ->
                db.activityRecordsQueries.insert(record.toSqlDelight())
            }
        }
    }

    override suspend fun search(query: String): List<ActivityRecord> {
        return withContext(ioDispatcher) {
            db.activityRecordsQueries.search(query).executeAsList().map { it.toDomain() }
        }
    }

    // override suspend fun getAllCompletedActivitiesForProject(
    //     projectId: String,
    //     goalIds: List<String>,
    // ): List<ActivityRecord> {
    //     return withContext(ioDispatcher) {
    //         db.activityRecordsQueries.getAllCompletedActivitiesForProject(projectId, goalIds)
    //             .executeAsList()
    //             .map { it.toDomain() }
    //     }
    // }

    override suspend fun findById(recordId: String): ActivityRecord? {
        return withContext(ioDispatcher) {
            db.activityRecordsQueries.findById(recordId).executeAsOneOrNull()?.toDomain()
        }
    }
}
