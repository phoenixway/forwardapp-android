package com.romankozak.forwardappmobile.shared.features.inbox

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InboxRecordRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : InboxRecordRepository {

    override fun getInboxRecords(projectId: String): Flow<List<InboxRecord>> {
        return db.inboxRecordQueries.selectAllByProjectId(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { inboxRecords -> inboxRecords.map { it.toDomain() } }
    }

    override suspend fun addInboxRecord(record: InboxRecord) {
        withContext(ioDispatcher) {
            db.inboxRecordQueries.insert(
                id = record.id,
                project_id = record.projectId,
                text = record.text,
                created_at = record.createdAt,
                item_order = record.order
            )
        }
    }

    override suspend fun deleteInboxRecord(id: String) {
        withContext(ioDispatcher) {
            db.inboxRecordQueries.deleteById(id)
        }
    }

    override suspend fun deleteAllByProjectId(projectId: String) {
        withContext(ioDispatcher) {
            db.inboxRecordQueries.deleteAllByProjectId(projectId)
        }
    }
}
