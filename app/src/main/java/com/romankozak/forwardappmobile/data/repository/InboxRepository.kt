package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxRepository
    @Inject
    constructor(
        private val inboxRecordDao: InboxRecordDao,
        private val goalRepository: GoalRepository,
    ) {
        suspend fun getInboxRecordById(id: String): InboxRecord? = inboxRecordDao.getRecordById(id)

        fun getInboxRecordsStream(contextId: String): Flow<List<InboxRecord>> = inboxRecordDao.getRecordsForContextStream(contextId)

        suspend fun addInboxRecord(
            text: String,
            contextId: String,
        ) {
            val currentTime = System.currentTimeMillis()
            val newRecord =
                InboxRecord(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    text = text,
                    createdAt = currentTime,
                    order = -currentTime,
                    updatedAt = currentTime,
                    syncedAt = null,
                    version = 1,
                )
            inboxRecordDao.insert(newRecord)
        }

        suspend fun updateInboxRecord(record: InboxRecord) {
        }

        suspend fun deleteInboxRecordById(recordId: String) {
            inboxRecordDao.deleteById(recordId)
        }

        @Transaction
        suspend fun promoteInboxRecordToGoal(record: InboxRecord) {
            goalRepository.addGoalToContext(record.text, record.contextId)
            deleteInboxRecordById(record.id)
        }

        @Transaction
        suspend fun promoteInboxRecordToGoal(
            record: InboxRecord,
            targetContextId: String,
        ) {
            goalRepository.addGoalToContext(record.text, targetContextId)
            deleteInboxRecordById(record.id)
        }
    }
