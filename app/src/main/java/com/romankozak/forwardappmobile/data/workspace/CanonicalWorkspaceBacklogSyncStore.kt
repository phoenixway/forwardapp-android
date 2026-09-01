package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySyncVersion
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import javax.inject.Inject
import javax.inject.Singleton

/** Canonical SnapshotBundle transport boundary for BACKLOG placements. */
@Singleton
class CanonicalWorkspaceBacklogSyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val entryDao: WorkspaceBacklogEntryDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val targetValidator: CanonicalBacklogTargetValidator,
    ) {
        suspend fun loadAll(): List<WorkspaceBacklogEntrySnapshot> =
            entryDao.getAll().map { it.toSnapshot() }

        suspend fun loadUnsynced(): List<WorkspaceBacklogEntrySnapshot> =
            entryDao.getUnsynced().map { it.toSnapshot() }

        suspend fun loadChangedSince(timestamp: Long): List<WorkspaceBacklogEntrySnapshot> =
            entryDao.getChangedSince(timestamp).map { it.toSnapshot() }

        suspend fun mergeIncoming(incoming: List<WorkspaceBacklogEntrySnapshot>?) {
            if (incoming == null || incoming.isEmpty()) return
            require(incoming.map { it.id }.distinct().size == incoming.size) {
                "Canonical BACKLOG payload contains duplicate ids"
            }

            database.withTransaction {
                val localById = entryDao.getAll().associateBy { it.id }
                val workspaceById = workspaceDao.getAll().associateBy { it.id }
                val capabilityById = orientationDao.getAllWorkspaceCapabilities().associateBy { it.id }

                incoming.forEach { candidate ->
                    validateCandidate(
                        candidate = candidate,
                        local = localById[candidate.id],
                        workspaceById = workspaceById,
                        capabilityById = capabilityById,
                    )
                }

                val winners =
                    incoming.filter { candidate ->
                        val local = localById[candidate.id] ?: return@filter true
                        candidate.version > local.version ||
                            (candidate.version == local.version && candidate.updatedAt > local.updatedAt) ||
                            (candidate.version == local.version &&
                                candidate.updatedAt == local.updatedAt && candidate.isDeleted && !local.isDeleted)
                    }
                if (winners.isEmpty()) return@withTransaction

                val resulting = localById.toMutableMap()
                winners.forEach { resulting[it.id] = it.toEntity() }
                validateLiveUniqueness(resulting.values)
                entryDao.upsert(winners.map { it.toEntity() })
            }
        }

        suspend fun markSynced(versions: List<WorkspaceBacklogEntrySyncVersion>) {
            if (versions.isEmpty()) return
            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                versions.forEach { sent ->
                    entryDao.markSyncedIfVersionMatches(sent.id, sent.version, syncedAt)
                }
            }
        }

        private suspend fun validateCandidate(
            candidate: WorkspaceBacklogEntrySnapshot,
            local: WorkspaceBacklogEntryEntity?,
            workspaceById: Map<String, WorkspaceEntity>,
            capabilityById: Map<String, WorkspaceCapabilityInstanceEntity>,
        ) {
            require(candidate.id.isNotBlank()) { "Canonical BACKLOG id must not be blank" }
            require(candidate.workspaceId.isNotBlank()) { "Canonical BACKLOG owner must not be blank" }
            require(candidate.capabilityInstanceId.isNotBlank()) { "Canonical BACKLOG capability must not be blank" }
            require(candidate.targetId.isNotBlank()) { "Canonical BACKLOG target must not be blank" }
            require(candidate.version >= 0L) { "Canonical BACKLOG version must not be negative" }
            require(candidate.createdAt >= 0L && candidate.updatedAt >= 0L) {
                "Canonical BACKLOG timestamps must not be negative"
            }
            val targetKind =
                runCatching { WorkspaceBacklogTargetKind.valueOf(candidate.targetKind) }
                    .getOrElse { error("Canonical BACKLOG target kind is unsupported: ${candidate.targetKind}") }

            val workspace = requireNotNull(workspaceById[candidate.workspaceId]) {
                "Canonical BACKLOG references missing Workspace ${candidate.workspaceId}"
            }
            require(!workspace.isDeleted || candidate.isDeleted) {
                "Live canonical BACKLOG cannot belong to a deleted Workspace"
            }
            val capability = requireNotNull(capabilityById[candidate.capabilityInstanceId]) {
                "Canonical BACKLOG references missing capability ${candidate.capabilityInstanceId}"
            }
            require(capability.workspaceId == candidate.workspaceId) {
                "Canonical BACKLOG capability belongs to another Workspace"
            }
            require(capability.capabilityType == WorkspaceCapabilityType.BACKLOG.name) {
                "Canonical BACKLOG row must reference a BACKLOG capability"
            }
            if (!candidate.isDeleted) {
                targetValidator.requireLive(WorkspaceBacklogTargetRef(targetKind, candidate.targetId))
            }
            local?.let { current ->
                require(current.targetKind == candidate.targetKind && current.targetId == candidate.targetId) {
                    "Canonical BACKLOG target identity is immutable: ${candidate.id}"
                }
                require(current.createdAt == candidate.createdAt) {
                    "Canonical BACKLOG createdAt is immutable: ${candidate.id}"
                }
            }
        }

        private fun validateLiveUniqueness(entries: Collection<WorkspaceBacklogEntryEntity>) {
            val live = entries.filterNot { it.isDeleted }
            require(live.groupBy { it.capabilityInstanceId to it.entryOrder }.none { it.value.size > 1 }) {
                "Canonical BACKLOG payload creates duplicate live order"
            }
            require(
                live.groupBy { Triple(it.capabilityInstanceId, it.targetKind, it.targetId) }
                    .none { it.value.size > 1 },
            ) {
                "Canonical BACKLOG payload creates duplicate live target placement"
            }
        }
    }

private fun WorkspaceBacklogEntryEntity.toSnapshot() =
    WorkspaceBacklogEntrySnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        targetKind = targetKind,
        targetId = targetId,
        order = entryOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceBacklogEntrySnapshot.toEntity() =
    WorkspaceBacklogEntryEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        targetKind = targetKind,
        targetId = targetId,
        entryOrder = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )
