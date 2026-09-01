package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceConnectionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalWorkspaceConnectionSyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val connectionDao: WorkspaceConnectionDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val attachmentDao: AttachmentDao,
    ) {
        suspend fun loadAll(): List<WorkspaceConnectionSnapshot> =
            connectionDao.getAll().map { it.toSnapshot() }

        suspend fun loadUnsynced(): List<WorkspaceConnectionSnapshot> =
            connectionDao.getUnsynced().map { it.toSnapshot() }

        suspend fun loadChangedSince(timestamp: Long): List<WorkspaceConnectionSnapshot> =
            connectionDao.getChangedSince(timestamp).map { it.toSnapshot() }

        suspend fun mergeIncoming(incoming: List<WorkspaceConnectionSnapshot>?) {
            if (incoming == null || incoming.isEmpty()) return
            require(incoming.map { it.id }.distinct().size == incoming.size) {
                "Canonical CONNECTIONS payload contains duplicate ids"
            }

            database.withTransaction {
                val localById = connectionDao.getAll().associateBy { it.id }
                val workspaceById = workspaceDao.getAll().associateBy { it.id }
                val capabilityById = orientationDao.getAllWorkspaceCapabilities().associateBy { it.id }
                val attachmentById = attachmentDao.getAll().associateBy { it.id }
                incoming.forEach { validate(it, localById[it.id], workspaceById, capabilityById, attachmentById) }

                val winners =
                    incoming.filter { candidate ->
                        val local = localById[candidate.id] ?: return@filter true
                        candidate.version > local.version ||
                            (candidate.version == local.version && candidate.updatedAt > local.updatedAt) ||
                            (candidate.version == local.version &&
                                candidate.updatedAt == local.updatedAt && candidate.isDeleted && !local.isDeleted)
                    }
                if (winners.isNotEmpty()) connectionDao.upsert(winners.map { it.toEntity() })
            }
        }

        suspend fun markSynced(versions: List<WorkspaceConnectionSyncVersion>) {
            if (versions.isEmpty()) return
            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                versions.forEach {
                    connectionDao.markSyncedIfVersionMatches(it.id, it.version, syncedAt)
                }
            }
        }

        private fun validate(
            candidate: WorkspaceConnectionSnapshot,
            local: WorkspaceConnectionEntity?,
            workspaceById: Map<String, WorkspaceEntity>,
            capabilityById: Map<String, WorkspaceCapabilityInstanceEntity>,
            attachmentById: Map<String, AttachmentEntity>,
        ) {
            require(candidate.id.isNotBlank()) { "Canonical CONNECTIONS id must not be blank" }
            require(candidate.workspaceId.isNotBlank()) { "Canonical CONNECTIONS owner must not be blank" }
            require(candidate.capabilityInstanceId.isNotBlank()) { "Canonical CONNECTIONS capability must not be blank" }
            require(candidate.attachmentId.isNotBlank()) { "Canonical CONNECTIONS attachment must not be blank" }
            require(candidate.order >= 0L || candidate.isDeleted) { "Live canonical CONNECTIONS order must not be negative" }
            require(candidate.version >= 0L) { "Canonical CONNECTIONS version must not be negative" }

            val workspace = requireNotNull(workspaceById[candidate.workspaceId]) {
                "Canonical CONNECTIONS references missing Workspace ${candidate.workspaceId}"
            }
            require(!workspace.isDeleted || candidate.isDeleted) {
                "Live canonical CONNECTIONS cannot belong to a deleted Workspace"
            }
            val capability = requireNotNull(capabilityById[candidate.capabilityInstanceId]) {
                "Canonical CONNECTIONS references missing capability ${candidate.capabilityInstanceId}"
            }
            require(capability.workspaceId == candidate.workspaceId) {
                "Canonical CONNECTIONS capability belongs to another Workspace"
            }
            require(capability.capabilityType == WorkspaceCapabilityType.CONNECTIONS.name) {
                "Canonical CONNECTIONS row must reference a CONNECTIONS capability"
            }
            val attachment = requireNotNull(attachmentById[candidate.attachmentId]) {
                "Canonical CONNECTIONS references missing Attachment ${candidate.attachmentId}"
            }
            require(!attachment.isDeleted || candidate.isDeleted) {
                "Live canonical CONNECTIONS cannot target a deleted Attachment"
            }
            local?.let {
                require(it.workspaceId == candidate.workspaceId) { "Canonical CONNECTIONS ownership is immutable" }
                require(it.capabilityInstanceId == candidate.capabilityInstanceId) {
                    "Canonical CONNECTIONS capability ownership is immutable"
                }
                require(it.attachmentId == candidate.attachmentId) {
                    "Canonical CONNECTIONS target Attachment is immutable"
                }
                require(it.createdAt == candidate.createdAt) { "Canonical CONNECTIONS createdAt is immutable" }
            }
        }
    }

private fun WorkspaceConnectionEntity.toSnapshot() =
    WorkspaceConnectionSnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        attachmentId = attachmentId,
        order = connectionOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceConnectionSnapshot.toEntity() =
    WorkspaceConnectionEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        attachmentId = attachmentId,
        connectionOrder = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )
