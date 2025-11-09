package com.romankozak.forwardappmobile.shared.features.activity_records.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.shared.database.ActivityRecords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActivityRecordRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ActivityRecordRepository {

    private val queries = db.activityRecordsQueries

    override suspend fun insert(record: ActivityRecord) {
        withContext(ioDispatcher) {
            queries.insert(
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
                parentProjectId = record.parentProjectId
            )
        }
    }

    override suspend fun update(record: ActivityRecord) {
        withContext(ioDispatcher) {
            queries.update(
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
                parentProjectId = record.parentProjectId
            )
        }
    }

    override fun getAllRecordsStream(): Flow<List<ActivityRecord>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records -> records.map { it.toDomain() } }
    }

    override suspend fun findLastOngoingActivity(): ActivityRecord? {
        return withContext(ioDispatcher) {
            queries.findLastOngoingActivity().executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun findLastOngoingActivityForProject(projectId: String): ActivityRecord? {
        return withContext(ioDispatcher) {
            queries.findLastOngoingActivityForProject(projectId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun clearAll() {
        withContext(ioDispatcher) {
            queries.clearAll()
        }
    }

    override suspend fun delete(record: ActivityRecord) {
        withContext(ioDispatcher) {
            queries.delete(record.id)
        }
    }

    override suspend fun insertAll(records: List<ActivityRecord>) {
        withContext(ioDispatcher) {
            records.forEach { record ->
                queries.insert(
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
                    parentProjectId = record.parentProjectId
                )
            }
        }
    }

    override suspend fun search(query: String): List<ActivityRecord> {
        return withContext(ioDispatcher) {
            queries.search(query).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun findById(recordId: String): ActivityRecord? {
        return withContext(ioDispatcher) {
            queries.findById(recordId).executeAsOneOrNull()?.toDomain()
        }
    }
}

fun ActivityRecords.toDomain(): ActivityRecord {
    return ActivityRecord(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        startTime = startTime,
        endTime = endTime,
        totalTimeSpentMinutes = totalTimeSpentMinutes,
        tags = tags,
        relatedLinks = relatedLinks,
        isCompleted = isCompleted,
        activityType = activityType,
        parentProjectId = parentProjectId
    )
}