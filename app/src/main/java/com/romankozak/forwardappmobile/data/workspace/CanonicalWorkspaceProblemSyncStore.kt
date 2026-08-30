package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemAttachmentRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemWorkspaceRefEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemAttachmentRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemAttachmentRefSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemWorkspaceRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemWorkspaceRefSyncVersion
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.capability.WorkspaceProblemDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncPayload
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.KeyProblemsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.validateKeyProblemsContract
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemAttachmentRef
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemWorkspaceRef
import javax.inject.Inject
import javax.inject.Singleton

fun SnapshotBundle.toCanonicalWorkspaceProblemSyncPayloadOrNull(): CanonicalWorkspaceProblemSyncPayload? {
    val presentFieldCount =
        listOf(
            workspaceProblems,
            workspaceProblemWorkspaceRefs,
            workspaceProblemAttachmentRefs,
        ).count { it != null }
    require(presentFieldCount == 0 || presentFieldCount == 3) {
        "Canonical KEY_PROBLEMS snapshot must contain either none or all three typed fields"
    }
    if (presentFieldCount == 0) return null
    return CanonicalWorkspaceProblemSyncPayload(
        problems = requireNotNull(workspaceProblems),
        workspaceRefs = requireNotNull(workspaceProblemWorkspaceRefs),
        attachmentRefs = requireNotNull(workspaceProblemAttachmentRefs),
    )
}

/**
 * Canonical KEY_PROBLEMS transport boundary.
 *
 * All three typed streams merge in one transaction. Incoming rows are first
 * checked for immutable identity and owner validity, then freshness winners are
 * projected into a complete final state and validated as one KEY_PROBLEMS
 * contract before anything is persisted.
 */
@Singleton
class CanonicalWorkspaceProblemSyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val problemDao: WorkspaceProblemDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val attachmentDao: AttachmentDao,
    ) {
        suspend fun loadAll(): CanonicalWorkspaceProblemSyncPayload =
            CanonicalWorkspaceProblemSyncPayload(
                problems = problemDao.getAllProblems().map { it.toSnapshot() },
                workspaceRefs = problemDao.getAllWorkspaceRefs().map { it.toSnapshot() },
                attachmentRefs = problemDao.getAllAttachmentRefs().map { it.toSnapshot() },
            )

        suspend fun loadUnsynced(): CanonicalWorkspaceProblemSyncPayload =
            CanonicalWorkspaceProblemSyncPayload(
                problems = problemDao.getUnsyncedProblems().map { it.toSnapshot() },
                workspaceRefs = problemDao.getUnsyncedWorkspaceRefs().map { it.toSnapshot() },
                attachmentRefs = problemDao.getUnsyncedAttachmentRefs().map { it.toSnapshot() },
            )

        suspend fun loadChangedSince(timestamp: Long): CanonicalWorkspaceProblemSyncPayload =
            CanonicalWorkspaceProblemSyncPayload(
                problems = problemDao.getProblemsChangedSince(timestamp).map { it.toSnapshot() },
                workspaceRefs =
                    problemDao.getWorkspaceRefsChangedSince(timestamp).map { it.toSnapshot() },
                attachmentRefs =
                    problemDao.getAttachmentRefsChangedSince(timestamp).map { it.toSnapshot() },
            )

        suspend fun mergeIncoming(payload: CanonicalWorkspaceProblemSyncPayload) {
            if (
                payload.problems.isEmpty() &&
                payload.workspaceRefs.isEmpty() &&
                payload.attachmentRefs.isEmpty()
            ) {
                return
            }

            database.withTransaction {
                requireUniqueIds(payload)

                val localProblems = problemDao.getAllProblems()
                val localWorkspaceRefs = problemDao.getAllWorkspaceRefs()
                val localAttachmentRefs = problemDao.getAllAttachmentRefs()

                val localProblemsById = localProblems.associateBy { it.id }
                val localWorkspaceRefsById = localWorkspaceRefs.associateBy { it.id }
                val localAttachmentRefsById = localAttachmentRefs.associateBy { it.id }

                val workspacesById = workspaceDao.getAll().associateBy { it.id }
                val capabilitiesById =
                    orientationDao.getAllWorkspaceCapabilities().associateBy { it.id }
                val attachmentsById = attachmentDao.getAll().associateBy { it.id }

                payload.problems.forEach { candidate ->
                    validateProblemCandidate(
                        candidate = candidate,
                        local = localProblemsById[candidate.id],
                        workspacesById = workspacesById,
                        capabilitiesById = capabilitiesById,
                    )
                }

                payload.workspaceRefs.forEach { candidate ->
                    validateWorkspaceRefCandidate(
                        candidate = candidate,
                        local = localWorkspaceRefsById[candidate.id],
                        workspacesById = workspacesById,
                    )
                }

                payload.attachmentRefs.forEach { candidate ->
                    validateAttachmentRefCandidate(
                        candidate = candidate,
                        local = localAttachmentRefsById[candidate.id],
                        attachmentsById = attachmentsById,
                    )
                }

                val problemWinners =
                    payload.problems.filter { incoming ->
                        val local = localProblemsById[incoming.id]
                        local == null || incomingWins(incoming, local)
                    }
                val workspaceRefWinners =
                    payload.workspaceRefs.filter { incoming ->
                        val local = localWorkspaceRefsById[incoming.id]
                        local == null || incomingWins(incoming, local)
                    }
                val attachmentRefWinners =
                    payload.attachmentRefs.filter { incoming ->
                        val local = localAttachmentRefsById[incoming.id]
                        local == null || incomingWins(incoming, local)
                    }

                val finalProblems =
                    localProblemsById.toMutableMap().apply {
                        problemWinners.forEach { put(it.id, it.toCanonicalEntity()) }
                    }.values.toList()

                val finalWorkspaceRefs =
                    localWorkspaceRefsById.toMutableMap().apply {
                        workspaceRefWinners.forEach { put(it.id, it.toCanonicalEntity()) }
                    }.values.toList()

                val finalAttachmentRefs =
                    localAttachmentRefsById.toMutableMap().apply {
                        attachmentRefWinners.forEach { put(it.id, it.toCanonicalEntity()) }
                    }.values.toList()

                validateFinalState(
                    problems = finalProblems,
                    workspaceRefs = finalWorkspaceRefs,
                    attachmentRefs = finalAttachmentRefs,
                    workspacesById = workspacesById,
                    capabilitiesById = capabilitiesById,
                    attachmentsById = attachmentsById,
                )

                if (problemWinners.isNotEmpty()) {
                    problemDao.upsertProblems(problemWinners.map { it.toCanonicalEntity() })
                }
                if (workspaceRefWinners.isNotEmpty()) {
                    problemDao.upsertWorkspaceRefs(
                        workspaceRefWinners.map { it.toCanonicalEntity() },
                    )
                }
                if (attachmentRefWinners.isNotEmpty()) {
                    problemDao.upsertAttachmentRefs(
                        attachmentRefWinners.map { it.toCanonicalEntity() },
                    )
                }
            }
        }

        suspend fun markSynced(ack: CanonicalWorkspaceProblemSyncAck) {
            if (
                ack.problems.isEmpty() &&
                ack.workspaceRefs.isEmpty() &&
                ack.attachmentRefs.isEmpty()
            ) {
                return
            }

            require(ack.problems.map { it.id }.distinct().size == ack.problems.size) {
                "KEY_PROBLEMS sync ACK contains duplicate Problem ids"
            }
            require(ack.workspaceRefs.map { it.id }.distinct().size == ack.workspaceRefs.size) {
                "KEY_PROBLEMS sync ACK contains duplicate Workspace-ref ids"
            }
            require(ack.attachmentRefs.map { it.id }.distinct().size == ack.attachmentRefs.size) {
                "KEY_PROBLEMS sync ACK contains duplicate Attachment-ref ids"
            }

            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                ack.problems.forEach { sent ->
                    problemDao.markProblemSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
                ack.workspaceRefs.forEach { sent ->
                    problemDao.markWorkspaceRefSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
                ack.attachmentRefs.forEach { sent ->
                    problemDao.markAttachmentRefSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
            }
        }

        private fun requireUniqueIds(payload: CanonicalWorkspaceProblemSyncPayload) {
            require(payload.problems.map { it.id }.distinct().size == payload.problems.size) {
                "Incoming KEY_PROBLEMS payload contains duplicate Problem ids"
            }
            require(
                payload.workspaceRefs.map { it.id }.distinct().size ==
                    payload.workspaceRefs.size,
            ) {
                "Incoming KEY_PROBLEMS payload contains duplicate Workspace-ref ids"
            }
            require(
                payload.attachmentRefs.map { it.id }.distinct().size ==
                    payload.attachmentRefs.size,
            ) {
                "Incoming KEY_PROBLEMS payload contains duplicate Attachment-ref ids"
            }
        }

        private fun validateProblemCandidate(
            candidate: WorkspaceProblemSnapshot,
            local: WorkspaceProblemEntity?,
            workspacesById: Map<String, com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity>,
            capabilitiesById: Map<String, WorkspaceCapabilityInstanceEntity>,
        ) {
            require(candidate.id.isNotBlank()) { "KEY_PROBLEMS Problem id must not be blank" }
            require(candidate.workspaceId.isNotBlank()) {
                "KEY_PROBLEMS Problem ${candidate.id} workspaceId must not be blank"
            }
            require(candidate.capabilityInstanceId.isNotBlank()) {
                "KEY_PROBLEMS Problem ${candidate.id} capabilityInstanceId must not be blank"
            }
            require(candidate.order >= 0L) {
                "KEY_PROBLEMS Problem ${candidate.id} order must not be negative"
            }
            require(candidate.version > 0L) {
                "KEY_PROBLEMS Problem ${candidate.id} version must be positive"
            }
            runCatching { WorkspaceProblemStatus.valueOf(candidate.status) }.getOrElse {
                throw IllegalArgumentException(
                    "KEY_PROBLEMS Problem ${candidate.id} has unsupported status ${candidate.status}",
                )
            }

            val workspace =
                requireNotNull(workspacesById[candidate.workspaceId]) {
                    "KEY_PROBLEMS Problem ${candidate.id} references missing owner Workspace ${candidate.workspaceId}"
                }
            require(candidate.isDeleted || !workspace.isDeleted) {
                "Live KEY_PROBLEMS Problem ${candidate.id} cannot belong to deleted Workspace ${workspace.id}"
            }

            val capability =
                requireNotNull(capabilitiesById[candidate.capabilityInstanceId]) {
                    "KEY_PROBLEMS Problem ${candidate.id} references missing capability ${candidate.capabilityInstanceId}"
                }
            require(capability.workspaceId == candidate.workspaceId) {
                "KEY_PROBLEMS Problem ${candidate.id} capability belongs to another Workspace"
            }
            require(capability.capabilityType == WorkspaceCapabilityType.KEY_PROBLEMS.name) {
                "KEY_PROBLEMS Problem ${candidate.id} capability has wrong type"
            }
            KeyProblemsCapabilityConfigurationCodec.validate(
                version = capability.configurationVersion,
                raw = capability.configuration,
            )

            local?.let { current ->
                require(current.workspaceId == candidate.workspaceId) {
                    "KEY_PROBLEMS Problem ownership cannot move between Workspaces: ${candidate.id}"
                }
                require(current.capabilityInstanceId == candidate.capabilityInstanceId) {
                    "KEY_PROBLEMS Problem capability ownership is immutable: ${candidate.id}"
                }
                require(current.createdAt == candidate.createdAt) {
                    "KEY_PROBLEMS Problem createdAt is immutable: ${candidate.id}"
                }
            }
        }

        private fun validateWorkspaceRefCandidate(
            candidate: WorkspaceProblemWorkspaceRefSnapshot,
            local: WorkspaceProblemWorkspaceRefEntity?,
            workspacesById: Map<String, com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity>,
        ) {
            require(candidate.id.isNotBlank()) {
                "KEY_PROBLEMS Workspace-ref id must not be blank"
            }
            require(candidate.problemId.isNotBlank()) {
                "KEY_PROBLEMS Workspace-ref ${candidate.id} problemId must not be blank"
            }
            require(candidate.targetWorkspaceId.isNotBlank()) {
                "KEY_PROBLEMS Workspace-ref ${candidate.id} targetWorkspaceId must not be blank"
            }
            require(candidate.version > 0L) {
                "KEY_PROBLEMS Workspace-ref ${candidate.id} version must be positive"
            }
            require(workspacesById[candidate.targetWorkspaceId] != null) {
                "KEY_PROBLEMS Workspace-ref ${candidate.id} references missing Workspace ${candidate.targetWorkspaceId}"
            }

            local?.let { current ->
                require(current.problemId == candidate.problemId) {
                    "KEY_PROBLEMS Workspace-ref problem identity is immutable: ${candidate.id}"
                }
                require(current.targetWorkspaceId == candidate.targetWorkspaceId) {
                    "KEY_PROBLEMS Workspace-ref target identity is immutable: ${candidate.id}"
                }
                require(current.createdAt == candidate.createdAt) {
                    "KEY_PROBLEMS Workspace-ref createdAt is immutable: ${candidate.id}"
                }
            }
        }

        private fun validateAttachmentRefCandidate(
            candidate: WorkspaceProblemAttachmentRefSnapshot,
            local: WorkspaceProblemAttachmentRefEntity?,
            attachmentsById: Map<String, com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity>,
        ) {
            require(candidate.id.isNotBlank()) {
                "KEY_PROBLEMS Attachment-ref id must not be blank"
            }
            require(candidate.problemId.isNotBlank()) {
                "KEY_PROBLEMS Attachment-ref ${candidate.id} problemId must not be blank"
            }
            require(candidate.attachmentId.isNotBlank()) {
                "KEY_PROBLEMS Attachment-ref ${candidate.id} attachmentId must not be blank"
            }
            require(candidate.version > 0L) {
                "KEY_PROBLEMS Attachment-ref ${candidate.id} version must be positive"
            }
            require(attachmentsById[candidate.attachmentId] != null) {
                "KEY_PROBLEMS Attachment-ref ${candidate.id} references missing Attachment ${candidate.attachmentId}"
            }

            local?.let { current ->
                require(current.problemId == candidate.problemId) {
                    "KEY_PROBLEMS Attachment-ref problem identity is immutable: ${candidate.id}"
                }
                require(current.attachmentId == candidate.attachmentId) {
                    "KEY_PROBLEMS Attachment-ref target identity is immutable: ${candidate.id}"
                }
                require(current.createdAt == candidate.createdAt) {
                    "KEY_PROBLEMS Attachment-ref createdAt is immutable: ${candidate.id}"
                }
            }
        }

        private fun validateFinalState(
            problems: List<WorkspaceProblemEntity>,
            workspaceRefs: List<WorkspaceProblemWorkspaceRefEntity>,
            attachmentRefs: List<WorkspaceProblemAttachmentRefEntity>,
            workspacesById: Map<String, com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity>,
            capabilitiesById: Map<String, WorkspaceCapabilityInstanceEntity>,
            attachmentsById: Map<String, com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity>,
        ) {
            problems.forEach { problem ->
                val workspace =
                    requireNotNull(workspacesById[problem.workspaceId]) {
                        "KEY_PROBLEMS final state has missing owner Workspace ${problem.workspaceId}"
                    }
                require(problem.isDeleted || !workspace.isDeleted) {
                    "Live KEY_PROBLEMS Problem ${problem.id} cannot belong to deleted Workspace ${workspace.id}"
                }

                val capability =
                    requireNotNull(capabilitiesById[problem.capabilityInstanceId]) {
                        "KEY_PROBLEMS final state has missing capability ${problem.capabilityInstanceId}"
                    }
                require(capability.workspaceId == problem.workspaceId) {
                    "KEY_PROBLEMS final state has cross-Workspace capability ownership for ${problem.id}"
                }
                require(capability.capabilityType == WorkspaceCapabilityType.KEY_PROBLEMS.name) {
                    "KEY_PROBLEMS final state has wrong capability type for ${problem.id}"
                }
                KeyProblemsCapabilityConfigurationCodec.validate(
                    version = capability.configurationVersion,
                    raw = capability.configuration,
                )
            }

            workspaceRefs.forEach { ref ->
                require(workspacesById[ref.targetWorkspaceId] != null) {
                    "KEY_PROBLEMS final state has missing Workspace target ${ref.targetWorkspaceId}"
                }
            }
            attachmentRefs.forEach { ref ->
                require(attachmentsById[ref.attachmentId] != null) {
                    "KEY_PROBLEMS final state has missing Attachment target ${ref.attachmentId}"
                }
            }

            val violations =
                validateKeyProblemsContract(
                    problems = problems.map { it.toModel() },
                    workspaceRefs = workspaceRefs.map { it.toModel() },
                    attachmentRefs = attachmentRefs.map { it.toModel() },
                )

            require(violations.isEmpty()) {
                "Incoming KEY_PROBLEMS violates canonical contract: " +
                    violations.joinToString { "${it.path}:${it.code}" }
            }
        }

        private fun incomingWins(
            incoming: WorkspaceProblemSnapshot,
            local: WorkspaceProblemEntity,
        ): Boolean =
            when {
                incoming.version != local.version -> incoming.version > local.version
                incoming.updatedAt != local.updatedAt -> incoming.updatedAt > local.updatedAt
                incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                else -> false
            }

        private fun incomingWins(
            incoming: WorkspaceProblemWorkspaceRefSnapshot,
            local: WorkspaceProblemWorkspaceRefEntity,
        ): Boolean =
            when {
                incoming.version != local.version -> incoming.version > local.version
                incoming.updatedAt != local.updatedAt -> incoming.updatedAt > local.updatedAt
                incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                else -> false
            }

        private fun incomingWins(
            incoming: WorkspaceProblemAttachmentRefSnapshot,
            local: WorkspaceProblemAttachmentRefEntity,
        ): Boolean =
            when {
                incoming.version != local.version -> incoming.version > local.version
                incoming.updatedAt != local.updatedAt -> incoming.updatedAt > local.updatedAt
                incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                else -> false
            }
    }

private fun WorkspaceProblemEntity.toSnapshot() =
    WorkspaceProblemSnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        title = title,
        description = description,
        status = status,
        order = problemOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceProblemWorkspaceRefEntity.toSnapshot() =
    WorkspaceProblemWorkspaceRefSnapshot(
        id = id,
        problemId = problemId,
        targetWorkspaceId = targetWorkspaceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceProblemAttachmentRefEntity.toSnapshot() =
    WorkspaceProblemAttachmentRefSnapshot(
        id = id,
        problemId = problemId,
        attachmentId = attachmentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun WorkspaceProblemSnapshot.toCanonicalEntity() =
    WorkspaceProblemEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        title = title,
        description = description,
        status = status,
        problemOrder = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )

private fun WorkspaceProblemWorkspaceRefSnapshot.toCanonicalEntity() =
    WorkspaceProblemWorkspaceRefEntity(
        id = id,
        problemId = problemId,
        targetWorkspaceId = targetWorkspaceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )

private fun WorkspaceProblemAttachmentRefSnapshot.toCanonicalEntity() =
    WorkspaceProblemAttachmentRefEntity(
        id = id,
        problemId = problemId,
        attachmentId = attachmentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
    )

private fun WorkspaceProblemEntity.toModel() =
    WorkspaceProblem(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        title = title,
        description = description,
        status = WorkspaceProblemStatus.valueOf(status),
        order = problemOrder,
    )

private fun WorkspaceProblemWorkspaceRefEntity.toModel() =
    WorkspaceProblemWorkspaceRef(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        problemId = problemId,
        targetWorkspaceId = targetWorkspaceId,
    )

private fun WorkspaceProblemAttachmentRefEntity.toModel() =
    WorkspaceProblemAttachmentRef(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        problemId = problemId,
        attachmentId = attachmentId,
    )
