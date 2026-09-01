package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemAttachmentRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemWorkspaceRefEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.KeyProblemsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.validateKeyProblemsContract
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemAttachmentRef
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemWorkspaceRef
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CanonicalWorkspaceProblemItem(
    val problem: WorkspaceProblem,
    val relatedWorkspaceIds: List<String>,
    val relatedAttachmentIds: List<String>,
)

/**
 * Canonical Android authoring boundary for KEY_PROBLEMS.
 *
 * WorkspaceProblem owns text, status, ordering, and capability ownership.
 * Typed ref rows own relation identity. Capability metadata lifecycle remains
 * owned by CanonicalCapabilityInstanceStore.
 */
@Singleton
class CanonicalKeyProblemsRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val instanceStore: CanonicalCapabilityInstanceStore,
        private val problemDao: WorkspaceProblemDao,
        private val workspaceDao: WorkspaceDao,
        private val attachmentDao: AttachmentDao,
    ) {
        suspend fun enable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): String = instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.disable(SPEC, workspaceId, now)

        suspend fun archive(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.restore(SPEC, workspaceId, now)

        /**
         * Deletes capability metadata only. Owned KEY_PROBLEMS content remains
         * persisted and may be recovered by a later re-enable.
         */
        suspend fun deleteCapability(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.delete(SPEC, workspaceId, now)

        /**
         * Tombstones all live KEY_PROBLEMS content owned by deleted Workspaces.
         *
         * Owner deletion is independent from capability active state, so this
         * intentionally does not use the normal authoring guard.
         */
        suspend fun tombstoneOwnedContentForWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int =
            database.withTransaction {
                val ownerIds = workspaceIds.map(String::trim).filter(String::isNotEmpty).toSet()
                if (ownerIds.isEmpty()) return@withTransaction 0

                val problems = ownerIds.flatMap { problemDao.getLiveProblems(it) }
                if (problems.isEmpty()) return@withTransaction 0

                val workspaceRefs =
                    problems.flatMap { problem ->
                        problemDao.getWorkspaceRefs(problem.id)
                            .filterNot { it.isDeleted }
                            .map { it.bump(now).copy(isDeleted = true) }
                    }
                val attachmentRefs =
                    problems.flatMap { problem ->
                        problemDao.getAttachmentRefs(problem.id)
                            .filterNot { it.isDeleted }
                            .map { it.bump(now).copy(isDeleted = true) }
                    }

                problemDao.upsertProblems(problems.map { it.bump(now).copy(isDeleted = true) })
                if (workspaceRefs.isNotEmpty()) problemDao.upsertWorkspaceRefs(workspaceRefs)
                if (attachmentRefs.isNotEmpty()) problemDao.upsertAttachmentRefs(attachmentRefs)
                validatePersistedContract()
                problems.size
            }

        fun observeItems(workspaceId: String): Flow<List<CanonicalWorkspaceProblemItem>> =
            combine(
                problemDao.observeLiveProblems(workspaceId),
                problemDao.observeLiveWorkspaceRefs(workspaceId),
                problemDao.observeLiveAttachmentRefs(workspaceId),
            ) { problems, workspaceRefs, attachmentRefs ->
                toReadItems(problems, workspaceRefs, attachmentRefs)
            }

        suspend fun getItems(workspaceId: String): List<CanonicalWorkspaceProblemItem> =
            toReadItems(
                problems = problemDao.getLiveProblems(workspaceId),
                workspaceRefs =
                    problemDao.getLiveProblems(workspaceId)
                        .flatMap { problemDao.getWorkspaceRefs(it.id) }
                        .filterNot { it.isDeleted },
                attachmentRefs =
                    problemDao.getLiveProblems(workspaceId)
                        .flatMap { problemDao.getAttachmentRefs(it.id) }
                        .filterNot { it.isDeleted },
            )

        suspend fun createProblem(
            workspaceId: String,
            title: String,
            description: String = "",
            status: WorkspaceProblemStatus = WorkspaceProblemStatus.OPEN,
            relatedWorkspaceIds: List<String> = emptyList(),
            relatedAttachmentIds: List<String> = emptyList(),
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val normalizedTitle = title.trim()
                val normalizedDescription = description.trim()
                val workspaceTargets = normalizeIds(relatedWorkspaceIds, "Workspace")
                val attachmentTargets = normalizeIds(relatedAttachmentIds, "Attachment")

                requireContent(
                    normalizedTitle,
                    normalizedDescription,
                    workspaceTargets,
                    attachmentTargets,
                )
                validateLiveTargets(workspaceTargets, attachmentTargets)

                val id = UUID.randomUUID().toString()
                val problem =
                    WorkspaceProblemEntity(
                        id = id,
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        title = normalizedTitle,
                        description = normalizedDescription,
                        status = status.name,
                        problemOrder = nextOrder(workspaceId),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )

                problemDao.upsertProblems(listOf(problem))
                upsertDesiredWorkspaceRefs(problem, emptyList(), workspaceTargets, now)
                upsertDesiredAttachmentRefs(problem, emptyList(), attachmentTargets, now)
                validatePersistedContract()
                id
            }

        /**
         * Updates only an existing live canonical Problem.
         *
         * Missing ids are never interpreted as create.
         */
        suspend fun updateProblem(
            workspaceId: String,
            problemId: String,
            title: String,
            description: String,
            status: WorkspaceProblemStatus,
            relatedWorkspaceIds: List<String>,
            relatedAttachmentIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val current = requireMutableProblem(workspaceId, problemId, capability.id)
                val normalizedTitle = title.trim()
                val normalizedDescription = description.trim()
                val workspaceTargets = normalizeIds(relatedWorkspaceIds, "Workspace")
                val attachmentTargets = normalizeIds(relatedAttachmentIds, "Attachment")

                requireContent(
                    normalizedTitle,
                    normalizedDescription,
                    workspaceTargets,
                    attachmentTargets,
                )
                validateLiveTargets(workspaceTargets, attachmentTargets)

                val changedProblem =
                    current.title != normalizedTitle ||
                        current.description != normalizedDescription ||
                        current.status != status.name

                if (changedProblem) {
                    problemDao.upsertProblems(
                        listOf(
                            current.bump(now).copy(
                                title = normalizedTitle,
                                description = normalizedDescription,
                                status = status.name,
                            ),
                        ),
                    )
                }

                upsertDesiredWorkspaceRefs(
                    problem = current,
                    existing = problemDao.getWorkspaceRefs(problemId),
                    desiredTargetIds = workspaceTargets,
                    now = now,
                )
                upsertDesiredAttachmentRefs(
                    problem = current,
                    existing = problemDao.getAttachmentRefs(problemId),
                    desiredTargetIds = attachmentTargets,
                    now = now,
                )
                validatePersistedContract()
            }
        }

        suspend fun deleteProblem(
            workspaceId: String,
            problemId: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val current = requireMutableProblem(workspaceId, problemId, capability.id)

                val workspaceRefs =
                    problemDao.getWorkspaceRefs(problemId)
                        .filterNot { it.isDeleted }
                        .map { it.bump(now).copy(isDeleted = true) }
                val attachmentRefs =
                    problemDao.getAttachmentRefs(problemId)
                        .filterNot { it.isDeleted }
                        .map { it.bump(now).copy(isDeleted = true) }

                problemDao.upsertProblems(
                    listOf(current.bump(now).copy(isDeleted = true)),
                )
                if (workspaceRefs.isNotEmpty()) {
                    problemDao.upsertWorkspaceRefs(workspaceRefs)
                }
                if (attachmentRefs.isNotEmpty()) {
                    problemDao.upsertAttachmentRefs(attachmentRefs)
                }

                compactOrder(workspaceId, now)
                validatePersistedContract()
            }
        }

        suspend fun reorderProblems(
            workspaceId: String,
            orderedProblemIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                require(orderedProblemIds.size == orderedProblemIds.distinct().size) {
                    "KEY_PROBLEMS reorder contains duplicate ids"
                }

                val current = problemDao.getLiveProblems(workspaceId)
                require(current.all { it.capabilityInstanceId == capability.id }) {
                    "KEY_PROBLEMS contains rows owned by another capability instance"
                }
                require(orderedProblemIds.toSet() == current.map { it.id }.toSet()) {
                    "KEY_PROBLEMS reorder must contain every live Problem exactly once"
                }

                val byId = current.associateBy { it.id }
                val changed =
                    orderedProblemIds.mapIndexedNotNull { index, id ->
                        val problem = byId.getValue(id)
                        val order = index.toLong()
                        if (problem.problemOrder == order) {
                            null
                        } else {
                            problem.bump(now).copy(problemOrder = order)
                        }
                    }

                if (changed.isNotEmpty()) {
                    problemDao.upsertProblems(changed)
                }
                validatePersistedContract()
            }
        }

        private suspend fun requireMutableProblem(
            workspaceId: String,
            problemId: String,
            capabilityInstanceId: String,
        ): WorkspaceProblemEntity {
            require(problemId.isNotBlank()) { "Problem id must not be blank" }
            val problem =
                requireNotNull(problemDao.getProblem(problemId)) {
                    "KEY_PROBLEMS Problem does not exist"
                }

            require(!problem.isDeleted) { "KEY_PROBLEMS Problem is deleted" }
            require(problem.workspaceId == workspaceId) {
                "KEY_PROBLEMS Problem belongs to another Workspace"
            }
            require(problem.capabilityInstanceId == capabilityInstanceId) {
                "KEY_PROBLEMS Problem belongs to another capability instance"
            }
            return problem
        }

        private suspend fun validateLiveTargets(
            workspaceIds: List<String>,
            attachmentIds: List<String>,
        ) {
            workspaceIds.forEach { id ->
                val target =
                    requireNotNull(workspaceDao.getById(id)) {
                        "Related Workspace $id does not exist"
                    }
                require(!target.isDeleted) {
                    "Cannot create a live KEY_PROBLEMS ref to deleted Workspace $id"
                }
            }

            attachmentIds.forEach { id ->
                val target =
                    requireNotNull(attachmentDao.getAttachmentById(id)) {
                        "Related Attachment $id does not exist"
                    }
                require(!target.isDeleted) {
                    "Cannot create a live KEY_PROBLEMS ref to deleted Attachment $id"
                }
            }
        }

        private suspend fun upsertDesiredWorkspaceRefs(
            problem: WorkspaceProblemEntity,
            existing: List<WorkspaceProblemWorkspaceRefEntity>,
            desiredTargetIds: List<String>,
            now: Long,
        ) {
            require(existing.map { it.targetWorkspaceId }.distinct().size == existing.size) {
                "KEY_PROBLEMS has duplicate Workspace refs for Problem ${problem.id}"
            }

            val existingByTarget = existing.associateBy { it.targetWorkspaceId }
            val desired = desiredTargetIds.toSet()
            val changed = mutableListOf<WorkspaceProblemWorkspaceRefEntity>()

            existing.forEach { ref ->
                when {
                    ref.targetWorkspaceId in desired && ref.isDeleted ->
                        changed += ref.bump(now).copy(isDeleted = false)

                    ref.targetWorkspaceId !in desired && !ref.isDeleted ->
                        changed += ref.bump(now).copy(isDeleted = true)
                }
            }

            desiredTargetIds.filterNot(existingByTarget::containsKey).forEach { targetId ->
                changed +=
                    WorkspaceProblemWorkspaceRefEntity(
                        id = workspaceRefId(problem.id, targetId),
                        problemId = problem.id,
                        targetWorkspaceId = targetId,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
            }

            if (changed.isNotEmpty()) {
                problemDao.upsertWorkspaceRefs(changed)
            }
        }

        private suspend fun upsertDesiredAttachmentRefs(
            problem: WorkspaceProblemEntity,
            existing: List<WorkspaceProblemAttachmentRefEntity>,
            desiredTargetIds: List<String>,
            now: Long,
        ) {
            require(existing.map { it.attachmentId }.distinct().size == existing.size) {
                "KEY_PROBLEMS has duplicate Attachment refs for Problem ${problem.id}"
            }

            val existingByTarget = existing.associateBy { it.attachmentId }
            val desired = desiredTargetIds.toSet()
            val changed = mutableListOf<WorkspaceProblemAttachmentRefEntity>()

            existing.forEach { ref ->
                when {
                    ref.attachmentId in desired && ref.isDeleted ->
                        changed += ref.bump(now).copy(isDeleted = false)

                    ref.attachmentId !in desired && !ref.isDeleted ->
                        changed += ref.bump(now).copy(isDeleted = true)
                }
            }

            desiredTargetIds.filterNot(existingByTarget::containsKey).forEach { targetId ->
                changed +=
                    WorkspaceProblemAttachmentRefEntity(
                        id = attachmentRefId(problem.id, targetId),
                        problemId = problem.id,
                        attachmentId = targetId,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
            }

            if (changed.isNotEmpty()) {
                problemDao.upsertAttachmentRefs(changed)
            }
        }

        private suspend fun compactOrder(
            workspaceId: String,
            now: Long,
        ) {
            val changed =
                problemDao.getLiveProblems(workspaceId)
                    .sortedWith(compareBy<WorkspaceProblemEntity> { it.problemOrder }.thenBy { it.id })
                    .mapIndexedNotNull { index, problem ->
                        val expected = index.toLong()
                        if (problem.problemOrder == expected) {
                            null
                        } else {
                            problem.bump(now).copy(problemOrder = expected)
                        }
                    }

            if (changed.isNotEmpty()) {
                problemDao.upsertProblems(changed)
            }
        }

        private suspend fun nextOrder(workspaceId: String): Long =
            (problemDao.getLiveProblems(workspaceId).maxOfOrNull { it.problemOrder } ?: -1L) + 1L

        private suspend fun validatePersistedContract() {
            val violations =
                validateKeyProblemsContract(
                    problems = problemDao.getAllProblems().map { it.toModel() },
                    workspaceRefs = problemDao.getAllWorkspaceRefs().map { it.toModel() },
                    attachmentRefs = problemDao.getAllAttachmentRefs().map { it.toModel() },
                )

            require(violations.isEmpty()) {
                "KEY_PROBLEMS canonical contract violation: " +
                    violations.joinToString { "${it.path}:${it.code}" }
            }
        }

        private fun toReadItems(
            problems: List<WorkspaceProblemEntity>,
            workspaceRefs: List<WorkspaceProblemWorkspaceRefEntity>,
            attachmentRefs: List<WorkspaceProblemAttachmentRefEntity>,
        ): List<CanonicalWorkspaceProblemItem> {
            val workspacesByProblem =
                workspaceRefs.filterNot { it.isDeleted }
                    .groupBy { it.problemId }
            val attachmentsByProblem =
                attachmentRefs.filterNot { it.isDeleted }
                    .groupBy { it.problemId }

            return problems.filterNot { it.isDeleted }
                .sortedWith(compareBy<WorkspaceProblemEntity> { it.problemOrder }.thenBy { it.id })
                .map { problem ->
                    CanonicalWorkspaceProblemItem(
                        problem = problem.toModel(),
                        relatedWorkspaceIds =
                            workspacesByProblem[problem.id]
                                .orEmpty()
                                .map { it.targetWorkspaceId }
                                .distinct(),
                        relatedAttachmentIds =
                            attachmentsByProblem[problem.id]
                                .orEmpty()
                                .map { it.attachmentId }
                                .distinct(),
                    )
                }
        }

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.KEY_PROBLEMS,
                    configurationCodec = KeyProblemsCapabilityConfigurationCodec,
                    workspaceAuthority =
                        CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }
    }

private fun normalizeIds(
    values: List<String>,
    label: String,
): List<String> {
    val normalized = values.map(String::trim)
    require(normalized.none(String::isEmpty)) {
        "$label ids must not contain blanks"
    }
    return normalized.distinct()
}

private fun requireContent(
    title: String,
    description: String,
    workspaceIds: List<String>,
    attachmentIds: List<String>,
) {
    require(
        title.isNotBlank() ||
            description.isNotBlank() ||
            workspaceIds.isNotEmpty() ||
            attachmentIds.isNotEmpty(),
    ) {
        "KEY_PROBLEMS Problem must contain text or at least one relation"
    }
}

private fun workspaceRefId(
    problemId: String,
    workspaceId: String,
): String =
    "KEY_PROBLEM_WORKSPACE_REF:${problemId.length}:$problemId:${workspaceId.length}:$workspaceId"

private fun attachmentRefId(
    problemId: String,
    attachmentId: String,
): String =
    "KEY_PROBLEM_ATTACHMENT_REF:${problemId.length}:$problemId:${attachmentId.length}:$attachmentId"

private fun WorkspaceProblemEntity.bump(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

private fun WorkspaceProblemWorkspaceRefEntity.bump(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

private fun WorkspaceProblemAttachmentRefEntity.bump(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
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
