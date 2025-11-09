package com.romankozak.forwardappmobile.shared.features.inbox.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InboxRecordRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : InboxRecordRepository {

    override fun getRecordsForProjectStream(projectId: String): Flow<List<InboxRecord>> {
        return db.inboxRecordsQueries.getRecordsForProject(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records -> records.map { it.toDomain() } }
    }

    override suspend fun getRecordById(id: String): InboxRecord? {
        return withContext(ioDispatcher) {
            db.inboxRecordsQueries.getRecordById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun insert(record: InboxRecord) {
        withContext(ioDispatcher) {
            db.inboxRecordsQueries.insert(record.toSqlDelight())
        }
    }

    override suspend fun update(record: InboxRecord) {
        withContext(ioDispatcher) {
            db.inboxRecordsQueries.update(record.toSqlDelight())
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(ioDispatcher) {
            db.inboxRecordsQueries.deleteById(id)
        }
    }

    override suspend fun searchInboxRecordsGlobal(query: String): List<InboxRecord> {
        return withContext(ioDispatcher) {
            db.inboxRecordsQueries.searchInboxRecordsGlobal(query).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<InboxRecord> {
        return withContext(ioDispatcher) {
            db.inboxRecordsQueries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun insertAll(records: List<InboxRecord>) {
        withContext(ioDispatcher) {
            records.forEach { record ->
                db.inboxRecordsQueries.insert(record.toSqlDelight())
            }
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            db.inboxRecordsQueries.deleteAll()
        }
    }
}
