package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementMove
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal data class CanonicalWorkspacePresentation(
    val id: String,
    val nameOverride: String?,
    val descriptionOverride: String?,
    val roleCode: String?,
    val isDeleted: Boolean,
)

@Singleton
class CanonicalWorkspaceRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val executionLogRepository: CanonicalExecutionLogRepository,
        private val keyProblemsRepository: CanonicalKeyProblemsRepository,
        private val directionRepository: CanonicalDirectionRepository,
        private val inboxRepository: CanonicalInboxRepository,
        private val connectionsRepository: CanonicalConnectionsRepository,
        private val backlogRepository: CanonicalBacklogRepository,
        private val hierarchyPlacementLifecycleCoordinator: HierarchyPlacementLifecycleCoordinator,
        private val hierarchyPlacementRepository: CanonicalHierarchyPlacementRepository,
    ) {
        internal suspend fun getCanonicalPresentation(id: String): CanonicalWorkspacePresentation? =
            workspaceDao.getById(id)?.toCanonicalPresentationOrNull()

        suspend fun hasLiveWorkspace(id: String): Boolean =
            workspaceDao.getById(id)?.let { !it.isDeleted } ?: false

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


        /** Read-only V2 preset preflight. No target or placement is created. */
        internal suspend fun requirePresetParentOccurrence(
            parentWorkspaceId: String,
            parentPlacementId: PlacementId,
        ) {
            val parent = requireNotNull(hierarchyPlacementRepository.getPlacement(parentPlacementId)) {
                "Preset parent occurrence is missing: $parentPlacementId"
            }
            require(parent.hierarchyId == HierarchyId.GENERAL) {
                "Preset parent occurrence must belong to GENERAL"
            }
            require(
                parent.target ==
                    HierarchyTargetRef(HierarchyTargetType.WORKSPACE, parentWorkspaceId),
            ) {
                "Preset parent target and occurrence disagree"
            }
            requireActiveCanonical(parentWorkspaceId)
        }

        /**
         * V2 role-based ensure is scoped to one exact parent occurrence.
         * Reuses a live child target already visible directly under this occurrence;
         * never resolves parent PRIMARY from a target ID and never writes legacy topology.
         */
        internal suspend fun ensureChildWorkspaceByRoleAtOccurrence(
            parentWorkspaceId: String,
            parentPlacementId: PlacementId,
            roleCode: String,
            title: String,
        ): String =
            database.withTransaction {
                requirePresetParentOccurrence(parentWorkspaceId, parentPlacementId)

                val role = roleCode.trim()
                require(role.isNotEmpty()) { "Preset child role must not be blank" }
                val existing =
                    hierarchyPlacementRepository.getLiveChildren(parentPlacementId)
                        .filter { it.target.type == HierarchyTargetType.WORKSPACE }
                        .mapNotNull { child ->
                            workspaceDao.getById(child.target.id)
                                ?.takeUnless { it.isDeleted }
                                ?.takeIf { it.roleCode == role }
                                ?.id
                        }
                        .distinct()
                require(existing.size <= 1) {
                    "Multiple preset children with role $role under occurrence $parentPlacementId"
                }
                existing.singleOrNull()
                    ?: createWithV2PrimaryAppearance(
                        nameOverride = title,
                        parentWorkspaceId = parentWorkspaceId,
                        parentPlacementId = parentPlacementId,
                        roleCode = role,
                    )
            }

        suspend fun create(
            nameOverride: String,
            descriptionOverride: String? = null,
            parentWorkspaceId: String? = null,
            roleCode: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String {
            require(parentWorkspaceId == null) {
                "Workspace creation requires an exact parent occurrence"
            }
            return createWithV2PrimaryAppearance(
                nameOverride = nameOverride,
                descriptionOverride = descriptionOverride,
                roleCode = roleCode,
                now = now,
            )
        }

        internal suspend fun createWithPrimaryAppearance(
            nameOverride: String,
            descriptionOverride: String? = null,
            parentWorkspaceId: String? = null,
            parentPlacementId: PlacementId? = null,
            roleCode: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            createWithV2PrimaryAppearance(
                nameOverride = nameOverride,
                descriptionOverride = descriptionOverride,
                parentWorkspaceId = parentWorkspaceId,
                parentPlacementId = parentPlacementId,
                roleCode = roleCode,
                now = now,
            )

        /**
         * P2-only Workspace creation. GENERAL topology is authored exclusively
         * by the new PRIMARY placement; legacy Workspace parent/order are not
         * derived from the requested V2 parent occurrence.
         *
         * [parentWorkspaceId] validates the UI target/occurrence pair. It is
         * never written as Workspace.parentWorkspaceId by this method.
         */
        internal suspend fun createWithV2PrimaryAppearance(
            nameOverride: String,
            descriptionOverride: String? = null,
            parentWorkspaceId: String? = null,
            parentPlacementId: PlacementId? = null,
            roleCode: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                require((parentWorkspaceId == null) == (parentPlacementId == null)) {
                    "V2 Workspace creation requires matching parent target and occurrence"
                }
                if (parentPlacementId != null) {
                    val parent = requireNotNull(
                        hierarchyPlacementRepository.getPlacement(parentPlacementId),
                    ) {
                        "V2 Workspace creation parent occurrence is missing: $parentPlacementId"
                    }
                    require(parent.hierarchyId == HierarchyId.GENERAL) {
                        "V2 Workspace creation requires a GENERAL parent occurrence"
                    }
                    require(
                        parent.target ==
                            HierarchyTargetRef(
                                type = HierarchyTargetType.WORKSPACE,
                                id = requireNotNull(parentWorkspaceId),
                            ),
                    ) {
                        "V2 Workspace creation parent target and occurrence disagree"
                    }
                    requireActiveCanonical(requireNotNull(parentWorkspaceId))
                }

                val id = createInCurrentTransaction(
                    nameOverride = nameOverride,
                    descriptionOverride = descriptionOverride,
                    roleCode = roleCode,
                    now = now,
                )

                hierarchyPlacementRepository.createPrimaryAppearanceInCurrentTransaction(
                    target =
                        HierarchyTargetRef(
                            type = HierarchyTargetType.WORKSPACE,
                            id = id,
                        ),
                    parentPlacementId = parentPlacementId,
                    now = now,
                )
                id
            }

        private suspend fun createInCurrentTransaction(
            nameOverride: String,
            descriptionOverride: String?,
            roleCode: String?,
            now: Long,
        ): String {
            val name = nameOverride.trim()
            require(name.isNotEmpty()) { "Standalone Workspace name must not be blank" }

            val id = UUID.randomUUID().toString()
            val workspace =
                WorkspaceEntity(
                    id = id,
                    nameOverride = name,
                    descriptionOverride = descriptionOverride.normalized(),
                    roleCode = roleCode.normalized(),
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.STANDALONE.name,
                    sourceContextId = null,
                )
            workspaceDao.upsert(listOf(workspace))
            return id
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

        /**
         * P2-only CUT: concrete GENERAL occurrences, never Workspace parent/order.
         * The source/target pairs are validated against current persisted placements
         * before the complete prospective V2 topology is committed atomically.
         */
        internal suspend fun moveV2Occurrences(
            sourcePlacementsByWorkspaceId: Map<String, PlacementId>,
            targetWorkspaceId: String,
            targetPlacementId: PlacementId,
            now: Long = System.currentTimeMillis(),
        ): List<String> = database.withTransaction {
            require(sourcePlacementsByWorkspaceId.isNotEmpty()) {
                "V2 Workspace move requires concrete source occurrences"
            }
            require(sourcePlacementsByWorkspaceId.values.distinct().size == sourcePlacementsByWorkspaceId.size) {
                "V2 Workspace move contains duplicate source occurrences"
            }
            requireActiveCanonical(targetWorkspaceId)
            val target = requireNotNull(hierarchyPlacementRepository.getPlacement(targetPlacementId)) {
                "V2 Workspace destination occurrence is missing"
            }
            require(target.hierarchyId == HierarchyId.GENERAL) {
                "V2 Workspace destination must be GENERAL"
            }
            require(target.target == HierarchyTargetRef(HierarchyTargetType.WORKSPACE, targetWorkspaceId)) {
                "V2 Workspace destination target and occurrence disagree"
            }

            val moves = sourcePlacementsByWorkspaceId.map { (workspaceId, placementId) ->
                requireActiveCanonical(workspaceId)
                require(!SystemContexts.isPinnedRoot(ContextId(workspaceId))) {
                    "Pinned System Workspace cannot be moved under another Workspace"
                }
                val source = requireNotNull(hierarchyPlacementRepository.getPlacement(placementId)) {
                    "V2 Workspace source occurrence is missing"
                }
                require(source.hierarchyId == HierarchyId.GENERAL) {
                    "V2 Workspace source must be GENERAL"
                }
                require(source.target == HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspaceId)) {
                    "V2 Workspace source target and occurrence disagree"
                }
                HierarchyPlacementMove(
                    placementId = placementId,
                    newParentPlacementId = targetPlacementId,
                )
            }

            hierarchyPlacementRepository.movePlacementsInCurrentTransaction(moves, now)
            sourcePlacementsByWorkspaceId.keys.toList()
        }

        /**
         * P2-only shallow COPY: fresh Workspace identity plus a PRIMARY under
         * the explicit destination occurrence, in one Room transaction.
         * Legacy Workspace ancestry is deliberately left unset.
         */
        internal suspend fun copyV2WorkspacesShallow(
            ids: Collection<String>,
            targetWorkspaceId: String,
            targetPlacementId: PlacementId,
            now: Long = System.currentTimeMillis(),
        ): List<String> = database.withTransaction {
            val requestedIds = ids.filter { it.isNotBlank() }.distinct()
            require(requestedIds.isNotEmpty()) { "V2 Workspace copy selection must not be empty" }
            requireActiveCanonical(targetWorkspaceId)
            val destination = requireNotNull(hierarchyPlacementRepository.getPlacement(targetPlacementId)) {
                "V2 Workspace copy destination occurrence is missing"
            }
            require(destination.hierarchyId == HierarchyId.GENERAL) {
                "V2 Workspace copy destination must be GENERAL"
            }
            require(destination.target == HierarchyTargetRef(HierarchyTargetType.WORKSPACE, targetWorkspaceId)) {
                "V2 Workspace copy destination target and occurrence disagree"
            }

            val sources = requestedIds.map { requireActiveCanonical(it) }
            val siblingNames = hierarchyPlacementRepository.getLiveChildren(targetPlacementId)
                .mapNotNull { placement ->
                    if (placement.target.type != HierarchyTargetType.WORKSPACE) null
                    else workspaceDao.getById(placement.target.id)?.nameOverride
                }
                .toMutableSet()

            sources.map { source ->
                val baseName = requireNotNull(source.nameOverride?.trim()?.takeIf { it.isNotEmpty() }) {
                    "Workspace copy source has no canonical display name: ${source.id}"
                }
                val copiedName = generateCopiedWorkspaceName(baseName, siblingNames)
                siblingNames += copiedName
                val id = createInCurrentTransaction(
                    nameOverride = copiedName,
                    descriptionOverride = null,
                    roleCode = source.roleCode,
                    now = now,
                )
                hierarchyPlacementRepository.createPrimaryAppearanceInCurrentTransaction(
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id),
                    parentPlacementId = targetPlacementId,
                    now = now,
                )
                id
            }
        }

        suspend fun tombstone(
            id: String,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            require(!SystemContexts.isSystem(ContextId(id))) {
                "Reserved System Workspace cannot be tombstoned"
            }

            val current = workspaceDao.getById(id) ?: error("Workspace does not exist")
            require(current.isCanonicalWorkspaceOwner()) {
                "Context-backed Workspace lifecycle remains owned by Context"
            }
            if (current.isDeleted) return@withTransaction

            // Only canonical placements determine structural children.
            // The lifecycle coordinator rejects any non-leaf appearance.
            directionRepository.tombstoneWorkspaceLinksTargeting(listOf(id), now)
            directionRepository.tombstoneOwnedEntriesForWorkspaces(listOf(id), now)
            keyProblemsRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            inboxRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            connectionsRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            backlogRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            executionLogRepository.tombstoneOwnedContentForWorkspaces(listOf(id), now)
            hierarchyPlacementLifecycleCoordinator.tombstoneWorkspaceTarget(id, now)
            workspaceDao.upsert(listOf(current.bump(now).copy(isDeleted = true)))

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

        private suspend fun requireActiveCanonical(id: String): WorkspaceEntity {
            val workspace = requireNotNull(workspaceDao.getById(id)) { "Workspace does not exist" }
            require(!workspace.isDeleted && workspace.isCanonicalWorkspaceOwner()) {
                "Workspace is not an active canonical Workspace"
            }
            return workspace
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
        roleCode = roleCode,
        isDeleted = isDeleted,
    )
}

private fun generateCopiedWorkspaceName(
    baseName: String,
    existingSiblingNames: Set<String>,
): String {
    val firstCandidate = "$baseName (копія)"
    if (firstCandidate !in existingSiblingNames) return firstCandidate

    var index = 2
    while (true) {
        val candidate = "$baseName (копія $index)"
        if (candidate !in existingSiblingNames) return candidate
        index += 1
    }
}

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
