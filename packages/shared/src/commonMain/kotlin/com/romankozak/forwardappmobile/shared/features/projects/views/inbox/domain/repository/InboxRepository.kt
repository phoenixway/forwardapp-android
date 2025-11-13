package com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import kotlinx.coroutines.flow.Flow

interface InboxRepository {
    fun observeInbox(projectId: String): Flow<List<InboxRecord>>
    suspend fun upsert(record: InboxRecord)
    suspend fun delete(recordId: String)
    suspend fun deleteAll(projectId: String? = null)
}
