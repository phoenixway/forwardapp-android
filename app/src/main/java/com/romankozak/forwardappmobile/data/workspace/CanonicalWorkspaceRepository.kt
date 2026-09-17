package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal data class CanonicalWorkspacePresentation(
    val id: String,
    val nameOverride: String?,
    val descriptionOverride: String?,
    val parentWorkspaceId: String?,
    val roleCode: String?,
    val workspaceOrder: Long,
    val isDeleted: Boolean,
)

/**
 * Read-only canonical node for ancestry and path construction.
 *
 * This intentionally excludes Context compatibility and all mutation state.
 * A node is available only when a live canonical Workspace has a usable
 * canonical display name.
 */
internal data class CanonicalWorkspaceAncestryPresentation(
    val id: String,
    val name: String,
    val parentWorkspaceId: String?,
)

internal data class CanonicalWorkspacePresentationUpdate(
    val id: String,
    val nameOverride: String?,
    val descriptionOverride: String?,
    val parentWorkspaceId: String?,
    val roleCode: String?,
    val workspaceOrder: Long,
)

internal data class CanonicalWorkspaceHierarchyUpdate(
    val id: String,
    val parentWorkspaceId: String?,
    val workspaceOrder: Long,
)

@Singleton
class CanonicalWorkspaceRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val graphRepository: CanonicalOrientationGraphRepository,
        private val executionLogRepository: CanonicalExecutionLogRepository,
        private val keyProblemsRepository: CanonicalKeyProblemsRepository,
        private val directionRepository: CanonicalDirectionRepository,
        private val inboxRepository: CanonicalInboxRepository,
        private val connectionsRepository: CanonicalConnectionsRepository,
        private val backlogRepository: CanonicalBacklogRepository,
    ) {
        /**
         * Resolves a live canonical Workspace without requiring a Context row.
         * CONTEXT_BACKED Workspaces remain owned by their Context presentation.
         */
        internal suspend fun getLiveCanonicalAncestryPresentation(
            id: String,
        ): CanonicalWorkspaceAncestryPresentation? =
            workspaceDao.getById(id)?.toLiveCanonicalAncestryPresentationOrNull()

        internal suspend fun getCanonicalPresentation(id: String): CanonicalWorkspacePresentation? =
            workspaceDao.getById(id)?.toCanonicalPresentationOrNull()

        internal suspend fun getCanonicalPresentations(): Map<String, CanonicalWorkspacePresentation> =
            workspaceDao.getAll()
                .mapNotNull { it.toCanonicalPresentationOrNull() }
                .associateBy { it.id }

        internal fun observeCanonicalPresentations(): Flow<Map<String, CanonicalWorkspacePresentation>> =
            workspaceDao.observeAll()
                .map { workspaces ->
                    workspaces
                        .mapNotNull { it.toCanonicalPresentationOrNull() }
                        .associateBy { it.id }
                }

        suspend fun create(
            nameOverride: String,
            descriptionOverride: String? = null,
            parentWorkspaceId: String? = null,
            roleCode: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val name = nameOverride.trim()
                require(name.isNotEmpty()) { "Standalone Workspace name must not be blank" }
                val live = loadLive()
                requireActiveParent(parentWorkspaceId, live)

                val id = UUID.randomUUID().toString()
                val workspace =
                    WorkspaceEntity(
                        id = id,
                        nameOverride = name,
                        descriptionOverride = descriptionOverride.normalized(),
                        parentWorkspaceId = parentWorkspaceId,
                        roleCode = roleCode.normalized(),
                        workspaceOrder = nextOrder(live.values, parentWorkspaceId),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        provenance = WorkspaceProvenance.STANDALONE.name,
                        sourceContextId = null,
                    )
                validateHierarchy(live.values + workspace)
                workspaceDao.upsert(listOf(workspace))
                id
            }

        suspend fun updateDetails(
            id: String,
            nameOverride: String?,
            descriptionOverride: String?,
            roleCode: String?,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            workspaceDao.upsert(
                listOf(
                    current.bump(now).copy(
                        nameOverride = nameOverride.normalized(),
                        descriptionOverride = descriptionOverride.normalized(),
                        roleCode = roleCode.normalized(),
                    ),
                ),
            )
        }

        suspend fun updateNameAndDescription(
            id: String,
            nameOverride: String?,
            descriptionOverride: String?,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            workspaceDao.upsert(
                listOf(
                    current.bump(now).copy(
                        nameOverride = nameOverride.normalized(),
                        descriptionOverride = descriptionOverride.normalized(),
                    ),
                ),
            )
        }

        suspend fun updateRole(
            id: String,
            roleCode: String?,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            workspaceDao.upsert(
                listOf(
                    current.bump(now).copy(
                        roleCode = roleCode.normalized(),
                    ),
                ),
            )
        }

        suspend fun move(
            id: String,
            newParentWorkspaceId: String?,
            order: Long? = null,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            val live = loadLive()
            requireActiveParent(newParentWorkspaceId, live)
            val changed =
                current.bump(now).copy(
                    parentWorkspaceId = newParentWorkspaceId,
                    workspaceOrder =
                        order ?: nextOrder(
                            live.values.filterNot { it.id == id },
                            newParentWorkspaceId,
                        ),
                )
            validateHierarchy(live.values.filterNot { it.id == id } + changed)
            workspaceDao.upsert(listOf(changed))
        }

        /**
         * Moves a canonical Workspace without allowing a legacy caller to choose its order.
         * The Workspace owner's current order is retained deliberately; [move]'s null order
         * instead means append beneath the new parent.
         */
        internal suspend fun movePreservingOrder(
            id: String,
            newParentWorkspaceId: String?,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            if (current.parentWorkspaceId == newParentWorkspaceId) return@withTransaction

            val live = loadLive()
            requireActiveParent(newParentWorkspaceId, live)
            val changed =
                current.bump(now).copy(
                    parentWorkspaceId = newParentWorkspaceId,
                    workspaceOrder = current.workspaceOrder,
                )
            validateHierarchy(live.values.filterNot { it.id == id } + changed)
            workspaceDao.upsert(listOf(changed))
        }

        /**
         * Applies a coherent set of canonical presentation/hierarchy changes.
         *
         * This is intentionally package-internal: compatibility bridges may
         * reuse the canonical Workspace owner's validation and versioning
         * without becoming a second Workspace lifecycle owner.
         *
         * Callers that already hold the surrounding Room transaction can use
         * this directly; all changes are validated as one prospective graph
         * and persisted as one batch.
         */
        internal suspend fun updatePresentationBatchInCurrentTransaction(
            updates: List<CanonicalWorkspacePresentationUpdate>,
            now: Long,
        ) {
            if (updates.isEmpty()) return

            require(updates.map { it.id }.distinct().size == updates.size) {
                "Canonical Workspace presentation batch contains duplicate ids"
            }

            val live = loadLive()
            val changedById = linkedMapOf<String, WorkspaceEntity>()

            updates.forEach { update ->
                val current =
                    requireNotNull(live[update.id]) {
                        "Workspace does not exist: ${update.id}"
                    }

                require(!current.isDeleted) {
                    "Workspace is deleted: ${update.id}"
                }
                require(current.isCanonicalWorkspaceOwner()) {
                    "Workspace presentation mutation requires canonical ownership: ${update.id}"
                }

                requireActiveParent(update.parentWorkspaceId, live)

                val desiredName = update.nameOverride.normalized()
                val desiredDescription = update.descriptionOverride.normalized()
                val desiredRole = update.roleCode.normalized()

                val presentationChanged =
                    current.nameOverride != desiredName ||
                        current.descriptionOverride != desiredDescription ||
                        current.parentWorkspaceId != update.parentWorkspaceId ||
                        current.roleCode != desiredRole ||
                        current.workspaceOrder != update.workspaceOrder

                if (!presentationChanged) return@forEach

                changedById[update.id] =
                    current.bump(now).copy(
                        nameOverride = desiredName,
                        descriptionOverride = desiredDescription,
                        parentWorkspaceId = update.parentWorkspaceId,
                        roleCode = desiredRole,
                        workspaceOrder = update.workspaceOrder,
                    )
            }

            if (changedById.isEmpty()) return

            val prospective =
                live.values.map { workspace ->
                    changedById[workspace.id] ?: workspace
                }

            validateHierarchy(prospective)
            workspaceDao.upsert(changedById.values.toList())
        }

        internal suspend fun updateHierarchyBatch(
            updates: List<CanonicalWorkspaceHierarchyUpdate>,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            if (updates.isEmpty()) return@withTransaction

            require(updates.map { it.id }.distinct().size == updates.size) {
                "Canonical Workspace hierarchy batch contains duplicate ids"
            }

            val live = loadLive()
            val changedById = linkedMapOf<String, WorkspaceEntity>()

            updates.forEach { update ->
                val current =
                    requireNotNull(live[update.id]) {
                        "Workspace does not exist: ${update.id}"
                    }

                require(!current.isDeleted) {
                    "Workspace is deleted: ${update.id}"
                }
                require(current.isCanonicalWorkspaceOwner()) {
                    "Workspace hierarchy mutation requires canonical ownership: ${update.id}"
                }

                requireActiveParent(update.parentWorkspaceId, live)

                if (
                    current.parentWorkspaceId == update.parentWorkspaceId &&
                    current.workspaceOrder == update.workspaceOrder
                ) {
                    return@forEach
                }

                changedById[update.id] =
                    current.bump(now).copy(
                        parentWorkspaceId = update.parentWorkspaceId,
                        workspaceOrder = update.workspaceOrder,
                    )
            }

            if (changedById.isEmpty()) return@withTransaction

            val prospective =
                live.values.map { workspace ->
                    changedById[workspace.id] ?: workspace
                }

            validateHierarchy(prospective)
            workspaceDao.upsert(changedById.values.toList())
        }

        suspend fun tombstone(
            id: String,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = workspaceDao.getById(id) ?: error("Workspace does not exist")
            require(current.isCanonicalWorkspaceOwner()) {
                "Context-backed Workspace lifecycle remains owned by Context"
            }
            if (current.isDeleted) return@withTransaction

            val live = loadLive()
            val children =
                live.values
                    .filter { it.parentWorkspaceId == id }
                    .sortedBy { it.workspaceOrder }
            val protectedChildren =
                children.filterNot { it.isCanonicalWorkspaceOwner() }
            require(protectedChildren.isEmpty()) {
                "Cannot tombstone canonical Workspace while it has Context-backed or unknown-provenance children"
            }

            val rootStart = nextOrder(live.values.filterNot { it.id == id }, null)
            val movedChildren =
                children.mapIndexed { index, child ->
                    child.bump(now).copy(
                        parentWorkspaceId = null,
                        workspaceOrder = rootStart + index,
                    )
                }

            validateHierarchy(
                live.values.filterNot { it.id == id || it.parentWorkspaceId == id } +
                    movedChildren,
            )

            directionRepository.tombstoneWorkspaceLinksTargeting(listOf(id), now)
            directionRepository.tombstoneOwnedEntriesForWorkspaces(listOf(id), now)
            keyProblemsRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            inboxRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            connectionsRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            backlogRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            executionLogRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            workspaceDao.upsert(movedChildren + current.bump(now).copy(isDeleted = true))

            orientationDao.upsertWorkspaceBindings(
                orientationDao.getAllWorkspaceBindings()
                    .filter { it.workspaceId == id && !it.isDeleted }
                    .map {
                        it.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = it.version + 1L,
                        )
                    },
            )
            orientationDao.upsertWorkspaceCapabilities(
                orientationDao.getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == id && !it.isDeleted }
                    .map {
                        it.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = it.version + 1L,
                        )
                    },
            )

        }

        suspend fun ensureEmbodiedWorkspace(
            subjectId: String,
            parentWorkspaceId: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                requireLiveSubject(subjectId)

                val bindings =
                    orientationDao.getAllWorkspaceBindings()
                        .filter {
                            !it.isDeleted &&
                                it.subjectId == subjectId &&
                                it.bindingType == WorkspaceBindingType.EMBODIES.name
                        }
                require(bindings.size <= 1) { "Subject has multiple embodied Workspaces" }

                bindings.singleOrNull()?.let { binding ->
                    val existing =
                        requireNotNull(workspaceDao.getById(binding.workspaceId)) {
                            "Embodied Workspace does not exist"
                        }
                    require(!existing.isDeleted) { "Embodied Workspace is deleted" }
                    return@withTransaction existing.id
                }

                val live = loadLive()
                requireActiveParent(parentWorkspaceId, live)
                val workspaceId = UUID.randomUUID().toString()
                workspaceDao.upsert(
                    listOf(
                        WorkspaceEntity(
                            id = workspaceId,
                            nameOverride = null,
                            descriptionOverride = null,
                            parentWorkspaceId = parentWorkspaceId,
                            roleCode = null,
                            workspaceOrder = nextOrder(live.values, parentWorkspaceId),
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                graphRepository.saveWorkspaceBindings(
                    listOf(
                        WorkspaceBinding(
                            id = UUID.randomUUID().toString(),
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            workspaceId = workspaceId,
                            subjectId = subjectId,
                            bindingType = WorkspaceBindingType.EMBODIES,
                            isPrimary = true,
                            order = 0L,
                        ),
                    ),
                )
                workspaceId
            }

        private suspend fun requireLiveSubject(id: String) {
            val subject = requireNotNull(orientationDao.getManagedSubject(id)) {
                "ManagedSubject does not exist"
            }
            require(!subject.isDeleted) { "ManagedSubject is deleted" }
            when (ManagedSubjectType.valueOf(subject.subjectType)) {
                ManagedSubjectType.ASPECT ->
                    require(orientationDao.getAspect(id) != null) { "Aspect node does not exist" }

                ManagedSubjectType.ORIENTATION ->
                    require(orientationDao.getAllOrientations().any { it.subjectId == id }) {
                        "Orientation node does not exist"
                    }
            }
        }

        private suspend fun requireActiveCanonical(id: String): WorkspaceEntity {
            val workspace = requireNotNull(workspaceDao.getById(id)) { "Workspace does not exist" }
            require(!workspace.isDeleted && workspace.isCanonicalWorkspaceOwner()) {
                "Workspace is not an active canonical Workspace"
            }
            return workspace
        }

        private suspend fun loadLive(): Map<String, WorkspaceEntity> =
            workspaceDao.getAll()
                .filterNot { it.isDeleted }
                .associateBy { it.id }

        private fun requireActiveParent(
            parentId: String?,
            live: Map<String, WorkspaceEntity>,
        ) {
            require(parentId == null || parentId in live) { "Workspace parent must be active" }
        }
    }

private fun WorkspaceEntity.toCanonicalPresentationOrNull(): CanonicalWorkspacePresentation? {
    if (!isCanonicalWorkspaceOwner()) return null
    require(sourceContextId == null) {
        "Canonical Workspace still references legacy Context: $id"
    }
    return CanonicalWorkspacePresentation(
        id = id,
        nameOverride = nameOverride,
        descriptionOverride = descriptionOverride,
        parentWorkspaceId = parentWorkspaceId,
        roleCode = roleCode,
        workspaceOrder = workspaceOrder,
        isDeleted = isDeleted,
    )
}

private fun WorkspaceEntity.toLiveCanonicalAncestryPresentationOrNull(): CanonicalWorkspaceAncestryPresentation? {
    if (
        isDeleted ||
        !isCanonicalWorkspaceOwner()
    ) {
        return null
    }
    val name = nameOverride?.takeIf { it.isNotBlank() } ?: return null
    return CanonicalWorkspaceAncestryPresentation(
        id = id,
        name = name,
        parentWorkspaceId = parentWorkspaceId,
    )
}

private fun validateHierarchy(workspaces: Collection<WorkspaceEntity>) {
    require(
        validateSingleParentHierarchy(
            workspaces.associate { it.id to it.parentWorkspaceId },
        ).isEmpty(),
    ) { "Workspace hierarchy violates DOMAIN-CONTRACT v1" }
}

private fun nextOrder(
    workspaces: Collection<WorkspaceEntity>,
    parentId: String?,
): Long =
    (workspaces.filter { it.parentWorkspaceId == parentId }
        .maxOfOrNull { it.workspaceOrder } ?: -1L) + 1L

private fun WorkspaceEntity.bump(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

private fun String?.normalized(): String? = this?.trim()?.ifEmpty { null }

private fun WorkspaceEntity.isCanonicalWorkspaceOwner(): Boolean =
    sourceContextId == null &&
        (
            provenance == WorkspaceProvenance.CANONICAL_ONLY.name ||
                (
                    provenance == WorkspaceProvenance.STANDALONE.name &&
                        !SystemContexts.isSystem(ContextId(id))
                )
        )
