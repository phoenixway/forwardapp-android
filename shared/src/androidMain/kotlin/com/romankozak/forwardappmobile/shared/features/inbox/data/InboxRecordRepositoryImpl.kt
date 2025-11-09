package com.romankozak.forwardappmobile.shared.features.inbox.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.shared.database.InboxRecords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InboxRecordRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : InboxRecordRepository {

    private val queries = db.inboxRecordsQueries

    override fun getRecordsForProjectStream(projectId: String): Flow<List<InboxRecord>> {
        return queries.getRecordsForProject(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records -> records.map { it.toDomain() } }
    }

    override suspend fun getRecordById(id: String): InboxRecord? {
        return withContext(ioDispatcher) {
            queries.getRecordById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun insert(record: InboxRecord) {
        withContext(ioDispatcher) {
            queries.insert(
                id = record.id,
                projectId = record.projectId,
                text = record.text,
                createdAt = record.createdAt,
                itemOrder = record.order
            )
        }
    }

    override suspend fun update(record: InboxRecord) {
        withContext(ioDispatcher) {
            queries.update(
                id = record.id,
                projectId = record.projectId,
                text = record.text,
                createdAt = record.createdAt,
                itemOrder = record.order
            )
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }

    override suspend fun searchInboxRecordsGlobal(query: String): List<InboxRecord> {
        return withContext(ioDispatcher) {
            queries.searchInboxRecordsGlobal(query).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<InboxRecord> {
        return withContext(ioDispatcher) {
            queries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun insertAll(records: List<InboxRecord>) {
        withContext(ioDispatcher) {
            records.forEach { record ->
                queries.insert(
                    id = record.id,
                    projectId = record.projectId,
                    text = record.text,
                    createdAt = record.createdAt,
                    itemOrder = record.order
                )
            }
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            queries.deleteAll()
        }
    }
}

fun InboxRecords.toDomain(): InboxRecord {
    return InboxRecord(
        id = id,
        projectId = projectId,
        text = text,
        createdAt = createdAt,
        order = itemOrder
    )
}