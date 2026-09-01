package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceInboxRecordEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalInboxRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val instanceStore: CanonicalCapabilityInstanceStore,
        private val recordDao: WorkspaceInboxRecordDao,
    ) {
        suspend fun enable(workspaceId: String, now: Long = System.currentTimeMillis()): String =
            instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.disable(SPEC, workspaceId, now)

        suspend fun archive(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.restore(SPEC, workspaceId, now)

        /** Capability metadata deletion preserves owned Inbox content. */
        suspend fun deleteCapability(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.delete(SPEC, workspaceId, now)

        suspend fun requireActive(workspaceId: String) {
            instanceStore.requireActiveInstance(SPEC, workspaceId)
        }

        fun observeRecords(workspaceId: String): Flow<List<WorkspaceInboxRecordEntity>> =
            recordDao.observeLive(workspaceId)

        suspend fun getRecords(workspaceId: String): List<WorkspaceInboxRecordEntity> =
            recordDao.getLive(workspaceId)

        suspend fun getRecord(id: String): WorkspaceInboxRecordEntity? = recordDao.getById(id)

        suspend fun getRecordsByIds(ids: List<String>): List<WorkspaceInboxRecordEntity> =
            if (ids.isEmpty()) emptyList() else recordDao.getByIds(ids)

        suspend fun createRecord(
            workspaceId: String,
            text: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val id = UUID.randomUUID().toString()
                recordDao.upsert(
                    listOf(
                        WorkspaceInboxRecordEntity(
                            id = id,
                            workspaceId = workspaceId,
                            capabilityInstanceId = capability.id,
                            text = text,
                            recordOrder = nextOrder(workspaceId),
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )
                id
            }

        suspend fun updateRecord(
            id: String,
            text: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val current = requireMutableRecord(id)
                instanceStore.requireActiveInstance(SPEC, current.workspaceId)
                if (current.text != text) {
                    recordDao.upsert(listOf(current.bump(now).copy(text = text)))
                }
            }
        }

        suspend fun reorder(
            workspaceId: String,
            orderedRecordIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                instanceStore.requireActiveInstance(SPEC, workspaceId)
                require(orderedRecordIds.size == orderedRecordIds.distinct().size) {
                    "Inbox reorder contains duplicate ids"
                }
                val current = recordDao.getLive(workspaceId)
                require(orderedRecordIds.toSet() == current.map { it.id }.toSet()) {
                    "Inbox reorder must contain every active record exactly once"
                }
                val byId = current.associateBy { it.id }
                val changes =
                    orderedRecordIds.mapIndexedNotNull { index, id ->
                        val record = byId.getValue(id)
                        val order = index.toLong()
                        record.takeIf { it.recordOrder != order }?.bump(now)?.copy(recordOrder = order)
                    }
                if (changes.isNotEmpty()) recordDao.upsert(changes)
            }
        }

        suspend fun tombstoneRecord(
            id: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val current = requireMutableRecord(id)
                instanceStore.requireActiveInstance(SPEC, current.workspaceId)
                recordDao.upsert(listOf(current.bump(now).copy(isDeleted = true)))
                compactOrder(current.workspaceId, now)
            }
        }

        suspend fun tombstoneMany(
            ids: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int {
            val requested = ids.map(String::trim).filter(String::isNotEmpty).distinct()
            if (requested.isEmpty()) return 0
            return database.withTransaction {
                val records = recordDao.getByIds(requested).filterNot { it.isDeleted }
                val workspaces = records.map { it.workspaceId }.distinct()
                workspaces.forEach { instanceStore.requireActiveInstance(SPEC, it) }
                if (records.isNotEmpty()) {
                    recordDao.upsert(records.map { it.bump(now).copy(isDeleted = true) })
                    workspaces.forEach { compactOrder(it, now) }
                }
                records.size
            }
        }

        /** Owner deletion bypasses the active-capability authoring guard. */
        suspend fun tombstoneOwnedContentForWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int {
            val owners = workspaceIds.map(String::trim).filter(String::isNotEmpty).toSet()
            if (owners.isEmpty()) return 0
            return database.withTransaction {
                val records = owners.flatMap { recordDao.getLive(it) }
                if (records.isNotEmpty()) {
                    recordDao.upsert(records.map { it.bump(now).copy(isDeleted = true) })
                }
                records.size
            }
        }

        private suspend fun requireMutableRecord(id: String): WorkspaceInboxRecordEntity {
            val record = requireNotNull(recordDao.getById(id)) { "Inbox record does not exist" }
            require(!record.isDeleted) { "Inbox record is deleted" }
            return record
        }

        private suspend fun nextOrder(workspaceId: String): Long =
            (recordDao.getLive(workspaceId).maxOfOrNull { it.recordOrder } ?: -1L) + 1L

        private suspend fun compactOrder(workspaceId: String, now: Long) {
            val changes =
                recordDao.getLive(workspaceId).mapIndexedNotNull { index, record ->
                    val order = index.toLong()
                    record.takeIf { it.recordOrder != order }?.bump(now)?.copy(recordOrder = order)
                }
            if (changes.isNotEmpty()) recordDao.upsert(changes)
        }

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.INBOX,
                    configurationCodec = InboxCapabilityConfigurationCodec,
                    workspaceAuthority = CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }
    }

private fun WorkspaceInboxRecordEntity.bump(now: Long) =
    copy(updatedAt = now, syncedAt = null, version = version + 1L)
