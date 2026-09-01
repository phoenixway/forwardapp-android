package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceInboxRecordEntity
import com.romankozak.forwardappmobile.data.logic.InboxAssociationCache
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Legacy-facing adapter over the canonical Workspace-owned INBOX collection. */
@Singleton
class InboxRepository
    @Inject
    constructor(
        private val canonicalRepository: CanonicalInboxRepository,
        private val inboxRecordLinkDao: InboxRecordLinkDao,
        private val goalRepository: GoalRepository,
        private val inboxAssociationCache: InboxAssociationCache,
    ) {
        suspend fun getInboxRecordById(id: String): InboxRecord? =
            canonicalRepository.getRecord(id)?.toCompatibility()

        fun getInboxRecordsStream(contextId: String): Flow<List<InboxRecord>> =
            inboxRecordLinkDao.getRecordsForContextStream(contextId)

        suspend fun addInboxRecord(
            text: String,
            contextId: String,
        ): String {
            val id = canonicalRepository.createRecord(contextId, text)
            canonicalRepository.getRecord(id)?.toCompatibility()?.let { inboxAssociationCache.refresh(it) }
            return id
        }

        suspend fun updateInboxRecord(record: InboxRecord) {
            canonicalRepository.updateRecord(record.id, record.text)
            canonicalRepository.getRecord(record.id)?.toCompatibility()?.let { inboxAssociationCache.refresh(it) }
        }

        suspend fun getInboxRecordsForContext(contextId: String): List<InboxRecord> =
            inboxRecordLinkDao.getRecordsForContext(contextId)

        suspend fun getInboxRecordsByIds(recordIds: List<String>): List<InboxRecord> {
            if (recordIds.isEmpty()) return emptyList()
            val byId = canonicalRepository.getRecordsByIds(recordIds).associateBy { it.id }
            return recordIds.mapNotNull { byId[it]?.toCompatibility() }
        }

        suspend fun updateInboxRecordsOrder(
            contextId: String,
            orders: Map<String, Long>,
        ) {
            if (orders.isEmpty()) return
            val orderedIds =
                canonicalRepository.getRecords(contextId)
                    .sortedWith(compareBy<WorkspaceInboxRecordEntity> { orders[it.id] ?: it.recordOrder }.thenBy { it.id })
                    .map { it.id }
            canonicalRepository.reorder(contextId, orderedIds)
        }

        suspend fun deleteInboxRecordById(recordId: String) {
            inboxAssociationCache.remove(recordId)
            val record = canonicalRepository.getRecord(recordId) ?: return
            if (record.isDeleted) return
            canonicalRepository.tombstoneRecord(
                id = recordId,
                now = maxOf(System.currentTimeMillis(), record.updatedAt + 1L),
            )
        }

        suspend fun deleteInboxRecordsByIds(recordIds: List<String>): Int {
            val uniqueIds = recordIds.distinct()
            uniqueIds.forEach { deleteInboxRecordById(it) }
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

private fun WorkspaceInboxRecordEntity.toCompatibility() =
    InboxRecord(
        id = id,
        contextId = workspaceId,
        text = text,
        createdAt = createdAt,
        order = recordOrder,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        hideInOwnerInbox = false,
        version = version,
    )
