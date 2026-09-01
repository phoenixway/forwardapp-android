package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceInboxRecordEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSyncVersion
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.capability.WorkspaceInboxRecordDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalWorkspaceInboxSyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val recordDao: WorkspaceInboxRecordDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
    ) {
        suspend fun loadAll(): List<WorkspaceInboxRecordSnapshot> =
            recordDao.getAll().map { it.toSnapshot() }

        suspend fun loadUnsynced(): List<WorkspaceInboxRecordSnapshot> =
            recordDao.getUnsynced().map { it.toSnapshot() }

        suspend fun loadChangedSince(timestamp: Long): List<WorkspaceInboxRecordSnapshot> =
            recordDao.getChangedSince(timestamp).map { it.toSnapshot() }

        suspend fun mergeIncoming(incoming: List<WorkspaceInboxRecordSnapshot>?) {
            if (incoming == null || incoming.isEmpty()) return
            require(incoming.map { it.id }.distinct().size == incoming.size) {
                "Canonical INBOX payload contains duplicate ids"
            }

            val localById = recordDao.getAll().associateBy { it.id }
            val workspaceById = workspaceDao.getAll().associateBy { it.id }
            val capabilityById = orientationDao.getAllWorkspaceCapabilities().associateBy { it.id }
            incoming.forEach { validate(it, localById[it.id], workspaceById, capabilityById) }

            val winners =
                incoming.filter { candidate ->
                    val local = localById[candidate.id] ?: return@filter true
                    candidate.version > local.version ||
                        (candidate.version == local.version && candidate.updatedAt > local.updatedAt) ||
                        (candidate.version == local.version &&
                            candidate.updatedAt == local.updatedAt && candidate.isDeleted && !local.isDeleted)
                }
            if (winners.isNotEmpty()) recordDao.upsert(winners.map { it.toEntity() })
        }

        suspend fun markSynced(versions: List<WorkspaceInboxRecordSyncVersion>) {
            if (versions.isEmpty()) return
            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                versions.forEach {
                    recordDao.markSyncedIfVersionMatches(it.id, it.version, syncedAt)
                }
            }
        }

        private fun validate(
            candidate: WorkspaceInboxRecordSnapshot,
            local: WorkspaceInboxRecordEntity?,
            workspaceById: Map<String, WorkspaceEntity>,
            capabilityById: Map<String, WorkspaceCapabilityInstanceEntity>,
        ) {
            require(candidate.id.isNotBlank()) { "Canonical INBOX id must not be blank" }
            require(candidate.workspaceId.isNotBlank()) { "Canonical INBOX owner must not be blank" }
            require(candidate.capabilityInstanceId.isNotBlank()) { "Canonical INBOX capability must not be blank" }
            require(candidate.order >= 0L || candidate.isDeleted) { "Live canonical INBOX order must not be negative" }
            require(candidate.version >= 0L) { "Canonical INBOX version must not be negative" }

            val workspace = requireNotNull(workspaceById[candidate.workspaceId]) {
                "Canonical INBOX references missing Workspace ${candidate.workspaceId}"
            }
            require(!workspace.isDeleted || candidate.isDeleted) {
                "Live canonical INBOX cannot belong to a deleted Workspace"
            }
            val capability = requireNotNull(capabilityById[candidate.capabilityInstanceId]) {
                "Canonical INBOX references missing capability ${candidate.capabilityInstanceId}"
            }
            require(capability.workspaceId == candidate.workspaceId) {
                "Canonical INBOX capability belongs to another Workspace"
            }
            require(capability.capabilityType == WorkspaceCapabilityType.INBOX.name) {
                "Canonical INBOX record must reference an INBOX capability"
            }
            local?.let {
                require(it.workspaceId == candidate.workspaceId) { "Canonical INBOX ownership is immutable" }
                require(it.capabilityInstanceId == candidate.capabilityInstanceId) {
                    "Canonical INBOX capability ownership is immutable"
                }
                require(it.createdAt == candidate.createdAt) { "Canonical INBOX createdAt is immutable" }
            }
        }
    }

private fun WorkspaceInboxRecordEntity.toSnapshot() =
    WorkspaceInboxRecordSnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        text = text,
        order = recordOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceInboxRecordSnapshot.toEntity() =
    WorkspaceInboxRecordEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        text = text,
        recordOrder = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )
