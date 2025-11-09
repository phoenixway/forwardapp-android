package com.romankozak.forwardappmobile.shared.features.inbox.data

import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import kotlinx.coroutines.flow.Flow

interface InboxRecordRepository {
    fun getRecordsForProjectStream(projectId: String): Flow<List<InboxRecord>>

    suspend fun getRecordById(id: String): InboxRecord?

    suspend fun insert(record: InboxRecord)

    suspend fun update(record: InboxRecord)

    suspend fun deleteById(id: String)

    suspend fun searchInboxRecordsGlobal(query: String): List<InboxRecord>

    suspend fun getAll(): List<InboxRecord>

    suspend fun insertAll(records: List<InboxRecord>)

    suspend fun deleteAll()
}
