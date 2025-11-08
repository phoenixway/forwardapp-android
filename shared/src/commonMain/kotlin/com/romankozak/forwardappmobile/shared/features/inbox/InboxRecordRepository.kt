package com.romankozak.forwardappmobile.shared.features.inbox

import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import kotlinx.coroutines.flow.Flow

interface InboxRecordRepository {
    fun getInboxRecords(projectId: String): Flow<List<InboxRecord>>
    suspend fun addInboxRecord(record: InboxRecord)
    suspend fun deleteInboxRecord(id: String)
    suspend fun deleteAllByProjectId(projectId: String)
}
