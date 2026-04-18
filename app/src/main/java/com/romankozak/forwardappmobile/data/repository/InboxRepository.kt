package com.romankozak.forwardappmobile.data.repository
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxRepository
    @Inject
    constructor(
        private val inboxRecordDao: InboxRecordDao,
        private val inboxRecordLinkDao: InboxRecordLinkDao,
        private val goalRepository: GoalRepository,
        private val tagAssociationHandler: TagAssociationHandler,
    ) {
        suspend fun getInboxRecordById(id: String): InboxRecord? = inboxRecordDao.getRecordById(id)

        fun getInboxRecordsStream(contextId: String): Flow<List<InboxRecord>> = inboxRecordLinkDao.getRecordsForContextStream(contextId)

        suspend fun addInboxRecord(
            text: String,
            contextId: String,
        ): String {
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
            tagAssociationHandler.syncInboxRecordAssociations(newRecord)
            return newRecord.id
        }

        suspend fun updateInboxRecord(record: InboxRecord) {
            inboxRecordDao.update(record)
            tagAssociationHandler.syncInboxRecordAssociations(record)
        }

        suspend fun getInboxRecordsForContext(contextId: String): List<InboxRecord> = inboxRecordLinkDao.getRecordsForContext(contextId)

        suspend fun getInboxRecordsByIds(recordIds: List<String>): List<InboxRecord> {
            if (recordIds.isEmpty()) return emptyList()
            val byId = inboxRecordDao.getAll().associateBy { it.id }
            return recordIds.mapNotNull(byId::get)
        }

        suspend fun updateInboxRecordsOrder(
            contextId: String,
            orders: Map<String, Long>,
        ) {
            if (orders.isEmpty()) return
            val now = System.currentTimeMillis()
            val existing = inboxRecordDao.getOwnedRecordsForContext(contextId)
            existing.forEach { record ->
                val nextOrder = orders[record.id] ?: return@forEach
                if (record.order == nextOrder) return@forEach
                inboxRecordDao.update(
                    record.copy(
                        order = nextOrder,
                        updatedAt = now,
                        syncedAt = null,
                        version = record.version + 1,
                    ),
                )
            }
        }

        suspend fun deleteInboxRecordById(recordId: String) {
            inboxRecordLinkDao.deleteForRecord(recordId)
            inboxRecordDao.deleteById(recordId)
        }

        suspend fun deleteInboxRecordsByIds(recordIds: List<String>): Int {
            val uniqueIds = recordIds.distinct()
            uniqueIds.forEach { inboxRecordDao.deleteById(it) }
            return uniqueIds.size
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
