package com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InboxRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : InboxRepository {

    override fun observeInbox(projectId: String): Flow<List<InboxRecord>> =
        database.inboxRecordsQueries.getInboxRecords(projectId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(record: InboxRecord) = withContext(dispatcher) {
        database.inboxRecordsQueries.insertInboxRecord(
            id = record.id,
            projectId = record.projectId,
            text = record.text,
            createdAt = record.createdAt,
            itemOrder = record.itemOrder,
        )
    }

    override suspend fun delete(recordId: String) = withContext(dispatcher) {
        database.inboxRecordsQueries.deleteInboxRecordById(recordId)
    }

    override suspend fun deleteAll(projectId: String?) = withContext(dispatcher) {
        if (projectId != null) {
            database.inboxRecordsQueries.deleteInboxRecordsByProject(projectId)
        } else {
            database.inboxRecordsQueries.deleteAllInboxRecords()
        }
    }
}
