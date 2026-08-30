package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySyncVersion
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical transport boundary for ordered DIRECTION placements.
 *
 * Schema 156 has one persistence authority. LEGACY_DIRECTION_ITEM is retained
 * only as historical provenance and is synchronized like CANONICAL_ONLY.
 */
@Singleton
class CanonicalWorkspaceDirectionEntrySyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val entryDao: WorkspaceDirectionEntryDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
    ) {
        suspend fun loadAll(): List<WorkspaceDirectionEntrySnapshot> =
            entryDao.getAll().map { it.toSnapshot() }

        suspend fun loadUnsynced(): List<WorkspaceDirectionEntrySnapshot> =
            entryDao.getUnsyncedForSync().map { it.toSnapshot() }

        suspend fun loadChangedSince(timestamp: Long): List<WorkspaceDirectionEntrySnapshot> =
            entryDao.getChangedSinceForSync(timestamp).map { it.toSnapshot() }

        /**
         * Workspace and Orientation canonical dependencies must be applied first.
         */
        suspend fun mergeIncoming(incoming: List<WorkspaceDirectionEntrySnapshot>?) {
            if (incoming == null || incoming.isEmpty()) return

            require(incoming.map { it.id }.toSet().size == incoming.size) {
                "Canonical DIRECTION entry payload contains duplicate ids"
            }
            require(incoming.all { it.hasSupportedProvenance() }) {
                "Canonical DIRECTION entry payload contains unsupported provenance"
            }

            val localById = entryDao.getAll().associateBy { it.id }
            val workspaceById = workspaceDao.getAll().associateBy { it.id }
            val capabilityById =
                orientationDao.getAllWorkspaceCapabilities().associateBy { it.id }
            val subjectById =
                orientationDao.getAllManagedSubjects().associateBy { it.id }
            val orientationById =
                orientationDao.getAllOrientations().associateBy { it.subjectId }

            incoming.forEach { candidate ->
                validateCanonicalCandidate(
                    candidate = candidate,
                    local = localById[candidate.id],
                    workspaceById = workspaceById,
                    capabilityById = capabilityById,
                    subjectById = subjectById,
                    orientationById = orientationById,
                )
            }

            val winners =
                incoming.filter { candidate ->
                    val local = localById[candidate.id] ?: return@filter true
                    incomingWins(candidate, local)
                }

            if (winners.isNotEmpty()) {
                entryDao.upsert(winners.map { it.toCanonicalEntity() })
            }
        }

        suspend fun markSynced(versions: List<WorkspaceDirectionEntrySyncVersion>) {
            if (versions.isEmpty()) return

            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                versions.forEach { sent ->
                    entryDao.markSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
            }
        }

        private fun validateCanonicalCandidate(
            candidate: WorkspaceDirectionEntrySnapshot,
            local: WorkspaceDirectionEntryEntity?,
            workspaceById: Map<String, WorkspaceEntity>,
            capabilityById: Map<String, WorkspaceCapabilityInstanceEntity>,
            subjectById: Map<String, ManagedSubjectEntity>,
            orientationById: Map<String, OrientationEntity>,
        ) {
            require(candidate.id.isNotBlank()) {
                "Canonical DIRECTION entry id must not be blank"
            }
            require(candidate.workspaceId.isNotBlank()) {
                "Canonical DIRECTION entry ${candidate.id} workspaceId must not be blank"
            }
            require(candidate.capabilityInstanceId.isNotBlank()) {
                "Canonical DIRECTION entry ${candidate.id} capabilityInstanceId must not be blank"
            }
            require(candidate.hasSupportedProvenance()) {
                "Canonical DIRECTION entry ${candidate.id} has unsupported provenance"
            }

            val hasOrientation = !candidate.orientationId.isNullOrBlank()
            val hasWorkspaceTarget = !candidate.targetWorkspaceId.isNullOrBlank()
            require(hasOrientation xor hasWorkspaceTarget) {
                "Canonical DIRECTION entry ${candidate.id} must have exactly one target"
            }

            val owner =
                requireNotNull(workspaceById[candidate.workspaceId]) {
                    "Canonical DIRECTION entry ${candidate.id} references missing owner Workspace ${candidate.workspaceId}"
                }
            require(!owner.isDeleted || candidate.isDeleted) {
                "Live canonical DIRECTION entry ${candidate.id} cannot belong to a deleted Workspace"
            }

            val capability =
                requireNotNull(capabilityById[candidate.capabilityInstanceId]) {
                    "Canonical DIRECTION entry ${candidate.id} references missing capability ${candidate.capabilityInstanceId}"
                }
            require(capability.workspaceId == candidate.workspaceId) {
                "Canonical DIRECTION entry ${candidate.id} capability belongs to another Workspace"
            }
            require(capability.capabilityType == WorkspaceCapabilityType.DIRECTION.name) {
                "Canonical DIRECTION entry ${candidate.id} must reference a DIRECTION capability"
            }

            candidate.orientationId?.let { orientationId ->
                require(orientationId.isNotBlank()) {
                    "Canonical DIRECTION entry ${candidate.id} orientationId must not be blank"
                }
                val subject =
                    requireNotNull(subjectById[orientationId]) {
                        "Canonical DIRECTION entry ${candidate.id} references missing Orientation subject $orientationId"
                    }
                val orientation =
                    requireNotNull(orientationById[orientationId]) {
                        "Canonical DIRECTION entry ${candidate.id} references missing Orientation $orientationId"
                    }
                require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                    "Canonical DIRECTION entry ${candidate.id} target is not an Orientation subject"
                }
                require(orientation.kind == OrientationKind.DIRECTION.name) {
                    "Canonical DIRECTION entry ${candidate.id} Orientation target is not DIRECTION kind"
                }
                require(!subject.isDeleted || candidate.isDeleted) {
                    "Live canonical DIRECTION entry ${candidate.id} cannot target a deleted Orientation"
                }
            }

            candidate.targetWorkspaceId?.let { targetWorkspaceId ->
                require(targetWorkspaceId.isNotBlank()) {
                    "Canonical DIRECTION entry ${candidate.id} targetWorkspaceId must not be blank"
                }
                val target =
                    requireNotNull(workspaceById[targetWorkspaceId]) {
                        "Canonical DIRECTION entry ${candidate.id} references missing target Workspace $targetWorkspaceId"
                    }
                require(!target.isDeleted || candidate.isDeleted) {
                    "Live canonical DIRECTION entry ${candidate.id} cannot target a deleted Workspace"
                }
            }

            local?.let { current ->
                require(current.workspaceId == candidate.workspaceId) {
                    "Canonical DIRECTION entry ownership cannot move between Workspaces: ${candidate.id}"
                }
                require(current.capabilityInstanceId == candidate.capabilityInstanceId) {
                    "Canonical DIRECTION entry capability ownership is immutable: ${candidate.id}"
                }
                require(
                    current.orientationId == candidate.orientationId &&
                        current.targetWorkspaceId == candidate.targetWorkspaceId,
                ) {
                    "Canonical DIRECTION entry target identity is immutable: ${candidate.id}"
                }
                require(current.provenance == candidate.provenance) {
                    "Canonical DIRECTION entry provenance is immutable: ${candidate.id}"
                }
                require(current.createdAt == candidate.createdAt) {
                    "Canonical DIRECTION entry createdAt is immutable: ${candidate.id}"
                }
            }
        }

        private fun incomingWins(
            incoming: WorkspaceDirectionEntrySnapshot,
            local: WorkspaceDirectionEntryEntity,
        ): Boolean =
            when {
                incoming.version != local.version ->
                    incoming.version > local.version
                incoming.updatedAt != local.updatedAt ->
                    incoming.updatedAt > local.updatedAt
                incoming.isDeleted != local.isDeleted ->
                    incoming.isDeleted
                else -> false
            }
    }

private fun WorkspaceDirectionEntrySnapshot.hasSupportedProvenance(): Boolean =
    provenance == WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name ||
        provenance == WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name

private fun WorkspaceDirectionEntryEntity.toSnapshot() =
    WorkspaceDirectionEntrySnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        orientationId = orientationId,
        targetWorkspaceId = targetWorkspaceId,
        labelOverride = labelOverride,
        entryOrder = entryOrder,
        provenance = provenance,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceDirectionEntrySnapshot.toCanonicalEntity() =
    WorkspaceDirectionEntryEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        orientationId = orientationId,
        targetWorkspaceId = targetWorkspaceId,
        labelOverride = labelOverride,
        entryOrder = entryOrder,
        provenance = provenance,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )
