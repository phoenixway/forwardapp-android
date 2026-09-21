package com.romankozak.forwardappmobile.core.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.normalizeLegacyStructuralContextBacklog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.currentHierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSnapshot
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.toCanonicalRows
import com.romankozak.forwardappmobile.data.orientation.toEffectiveOrientation
import com.romankozak.forwardappmobile.data.hierarchy.LegacyHierarchyRestoreTranslator
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.capability.isAdmittedLegacyContextIngressOwner
import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationBindings
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationPlanner
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogOwnerWorkspaceState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogTargetState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingLegacyPlanner
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingMigrationBindings
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyInboxSortingSource
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyBacklogItemSource
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyBacklogOrderSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import com.romankozak.forwardappmobile.sync.datasource.SnapshotRestoreCanonicalizer
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One-way restore compatibility. Merge/sync must never invoke this component. */
@Singleton
class SnapshotRestoreCanonicalizerImpl
    @Inject
    constructor() : SnapshotRestoreCanonicalizer {
        private val legacyHierarchyRestoreTranslator =
            LegacyHierarchyRestoreTranslator()

        override fun canonicalize(bundle: SnapshotBundle): SnapshotBundle =
            canonicalize(
                bundle = bundle,
                hierarchyAuthorityMode =
                    currentHierarchyPlacementAuthorityMode(),
            )

        internal fun canonicalize(
            bundle: SnapshotBundle,
            hierarchyAuthorityMode: HierarchyPlacementAuthorityMode,
        ): SnapshotBundle {
            val hasLegacyBacklog = bundle.backlogItems.isNotEmpty() || bundle.backlogOrders.isNotEmpty()
            val hasLegacyInbox = bundle.inbox.isNotEmpty()
            val hasLegacyInboxSorting = bundle.contextInboxSortingRules.isNotEmpty()
            val hasLegacySystemAppOwners =
                bundle.systemApps.any { app ->
                    app.workspaceId.isNullOrBlank() && !app.legacyContextId.isNullOrBlank()
                }
            val needsLegacyBacklog = bundle.workspaceBacklogEntries == null && hasLegacyBacklog
            val needsLegacyInbox = bundle.workspaceInboxRecords == null && hasLegacyInbox
            val needsLegacyInboxSorting =
                bundle.workspaceCapabilityInstances == null && hasLegacyInboxSorting
            val needsLegacySystemApps = hasLegacySystemAppOwners
            val needsLegacyOwners = bundle.workspaces == null && bundle.contexts.isNotEmpty()

            if (
                !needsLegacyOwners &&
                !needsLegacyBacklog &&
                !needsLegacyInbox &&
                !needsLegacyInboxSorting &&
                !needsLegacySystemApps
            ) {
                requireNoLiveOrdinaryContexts(bundle)
                return legacyHierarchyRestoreTranslator.translate(
                    source = bundle,
                    canonical = bundle,
                    authorityMode = hierarchyAuthorityMode,
                )
            }
            require(bundle.workspaces == null) {
                "Restore-only legacy content is ambiguous when a Workspace payload is already present"
            }

            val now = bundle.exportedAt.coerceAtLeast(1L)
            val sourceContexts = bundle.contexts.associateBy { it.id }
            require(sourceContexts.size == bundle.contexts.size) {
                "Restore Context evidence contains duplicate ids"
            }
            val sourceIds = sourceContexts.keys
            val parentById =
                sourceContexts.mapValues { (_, context) ->
                    context.parentId?.takeIf { it in sourceIds }
                }
            val cycleIds = cycleMembers(parentById)
            require(cycleIds.isEmpty()) {
                "Restore Context hierarchy contains a cycle: ${cycleIds.sorted()}"
            }

            val workspaces =
                bundle.contexts.map { context ->
                    WorkspaceEntity(
                        id = context.id,
                        nameOverride = context.name,
                        descriptionOverride = context.description,
                        parentWorkspaceId = parentById[context.id],
                        roleCode = context.roleCode,
                        workspaceOrder = context.order.toLong(),
                        createdAt = context.createdAt,
                        updatedAt = context.updatedAt,
                        syncedAt = null,
                        isDeleted = context.isDeleted,
                        version = context.version.coerceAtLeast(1L),
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    )
                }
            val liveWorkspaceIds = workspaces.filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.id }
            val canonicalSystemApps =
                canonicalizeLegacySystemApps(
                    bundle = bundle,
                    liveWorkspaceIds = liveWorkspaceIds,
                )

            val retiredContextEvidence =
                bundle.contexts
                    .filterNot { SystemContexts.isSystem(ContextId(it.id)) }
                    .map { context ->
                        if (context.isDeleted) context
                        else context.copy(
                            updatedAt = maxOf(now, context.updatedAt + 1L),
                            isDeleted = true,
                            version = nextVersion(context.version),
                        )
                    }

            val goalRows =
                bundle.goals.map { goal ->
                    goal.toEntity()
                        .toEffectiveOrientation(LegacySubjectUuid)
                        .toCanonicalRows(Gson(), GOAL_IDENTITY_CUTOVER_VERSION)
                }
            val goalMappings =
                goalRows.map { rows ->
                    rows.mapping.copy(state = LegacySubjectMappingState.CUT_OVER.name)
                }

            val configurationsByContextId =
                bundle.contextConfigurations
                    .filterNot { it.isDeleted }
                    .associateBy { it.contextId }
            require(bundle.workspaceCapabilityInstances == null) {
                "Restore-only Context-era canonicalization refuses a partial canonical capability payload"
            }
            val projectedCapabilities =
                projectCapabilities(
                    workspaces = workspaces,
                    contextsById = sourceContexts,
                    configurationsByContextId = configurationsByContextId,
                    backlogOwnerIds = bundle.backlogItems.mapTo(hashSetOf()) { it.contextId },
                    inboxOwnerIds = bundle.inbox.mapTo(hashSetOf()) { it.contextId },
                    inboxSortingOwnerIds =
                        bundle.contextInboxSortingRules.mapTo(hashSetOf()) { it.contextId },
                    now = now,
                )
            val capabilities =
                canonicalizeLegacyInboxSorting(
                    bundle = bundle,
                    workspaces = workspaces,
                    capabilities = projectedCapabilities,
                )
            val capabilityByOwnerAndType =
                capabilities.associateBy { it.workspaceId to it.capabilityType }

            val workspaceTags =
                bundle.contexts.flatMap { context ->
                    if (context.id !in liveWorkspaceIds) return@flatMap emptyList()
                    CanonicalWorkspaceTagRepository.normalizeTags(context.tags.orEmpty()).map { tag ->
                        WorkspaceTagRefEntity(
                            workspaceId = context.id,
                            normalizedTag = tag,
                            createdAt = context.createdAt,
                            updatedAt = context.updatedAt,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        )
                    }
                }

            val canonicalBacklogInput = bundle.workspaceBacklogEntries
            val canonicalBacklog =
                if (canonicalBacklogInput != null) {
                    canonicalBacklogInput
                } else {
                    canonicalizeLegacyBacklog(
                        bundle = bundle,
                        workspaces = workspaces,
                        capabilityByOwnerAndType = capabilityByOwnerAndType,
                        goalRows = goalRows,
                        goalMappings = goalMappings,
                        now = now,
                    )
                }
            val canonicalInboxInput = bundle.workspaceInboxRecords
            val canonicalInbox =
                if (canonicalInboxInput != null) {
                    canonicalInboxInput
                } else {
                    val canonicalLiveOrderById =
                        bundle.inbox
                            .filterNot { it.isDeleted }
                            .groupBy { it.contextId }
                            .values
                            .flatMap { ownerRecords ->
                                ownerRecords
                                    .sortedWith(
                                        compareBy(
                                            { it.order },
                                            { it.createdAt },
                                            { it.id },
                                        ),
                                    )
                                    .mapIndexed { index, source ->
                                        source.id to index.toLong()
                                    }
                            }.toMap()

                    bundle.inbox.map { source ->
                        val workspace =
                            workspaces.singleOrNull { candidate ->
                                isAdmittedLegacyContextIngressOwner(
                                    contextId = source.contextId,
                                    contextIsDeleted =
                                        sourceContexts[source.contextId]
                                            ?.takeUnless { SystemContexts.isSystem(ContextId(it.id)) }
                                            ?.let { true },
                                    workspace = candidate,
                                )
                            }
                        requireNotNull(workspace) {
                            "Legacy INBOX record ${source.id} has no unique live Workspace owner"
                        }
                        val capability =
                            requireNotNull(capabilityByOwnerAndType[workspace.id to WorkspaceCapabilityType.INBOX.name]) {
                                "Legacy INBOX record ${source.id} has no canonical INBOX anchor"
                            }
                        WorkspaceInboxRecordSnapshot(
                            id = source.id,
                            workspaceId = workspace.id,
                            capabilityInstanceId = capability.id,
                            text = source.text,
                            order =
                                if (source.isDeleted) {
                                    source.order
                                } else {
                                    requireNotNull(canonicalLiveOrderById[source.id]) {
                                        "Live legacy INBOX record ${source.id} has no canonical order"
                                    }
                                },
                            createdAt = source.createdAt,
                            updatedAt = source.updatedAt,
                            version = source.version,
                            isDeleted = source.isDeleted,
                        )
                    }
                }

            val result =
                bundle.copy(
                    contexts = retiredContextEvidence,
                    managedSubjects = goalRows.map { it.subject },
                    orientations = goalRows.map { it.orientation },
                    aspects = emptyList(),
                    orientationAssessments = goalRows.map { it.assessment },
                    orientationAssessmentRevisions = goalRows.map { it.revision },
                    legacySubjectMappings = goalMappings,
                    orientationRelations = emptyList(),
                    aspectOrientationRefs = emptyList(),
                    workspaces = workspaces,
                    workspaceBindings = emptyList(),
                    workspaceCapabilityInstances = capabilities,
                    workspaceTagRefs = workspaceTags,
                    savedOrientationViews = emptyList(),
                    workspaceBacklogEntries = canonicalBacklog,
                    workspaceInboxRecords = canonicalInbox,
                    systemApps = canonicalSystemApps,

                    // Restore-only compatibility evidence ends here. The
                    // canonical writer must never need Context-era BACKLOG,
                    // INBOX, or sorting-policy rows.
                    backlogItems = emptyList(),
                    backlogOrders = emptyList(),
                    inbox = emptyList(),
                    contextInboxSortingRules = emptyList(),
                )
            requireNoLiveOrdinaryContexts(result)
            require(result.contexts.none { SystemContexts.isSystem(ContextId(it.id)) }) {
                "Restore canonicalization retained a reserved System Context shell"
            }
            return legacyHierarchyRestoreTranslator.translate(
                source = bundle,
                canonical = result,
                authorityMode = hierarchyAuthorityMode,
            )
        }

        private fun canonicalizeLegacyBacklog(
            bundle: SnapshotBundle,
            workspaces: List<WorkspaceEntity>,
            capabilityByOwnerAndType: Map<Pair<String, String>, WorkspaceCapabilityInstanceEntity>,
            goalRows: List<com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRows>,
            goalMappings: List<com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity>,
            now: Long,
        ): List<WorkspaceBacklogEntrySnapshot> {
            val contextsById = bundle.contexts.associateBy { it.id }
            val ordinaryItems =
                bundle.backlogItems
                    .filterNot { SystemContexts.isSystem(ContextId(it.contextId)) }
                    .map { it.toEntity() }
            val ordinaryOrders =
                bundle.backlogOrders
                    .filterNot { SystemContexts.isSystem(ContextId(it.listId)) }
                    .map { it.toEntity() }
            val normalized =
                normalizeLegacyStructuralContextBacklog(
                    backlogItems = ordinaryItems,
                    backlogOrders = ordinaryOrders,
                    parentByContextId = contextsById.mapValues { it.value.parentId },
                    now = now,
                )
            val systemItems =
                bundle.backlogItems
                    .filter { SystemContexts.isSystem(ContextId(it.contextId)) }
                    .map { it.toEntity() }
            val systemOrders =
                bundle.backlogOrders
                    .filter { SystemContexts.isSystem(ContextId(it.listId)) }
                    .map { it.toEntity() }
            val items = normalized.backlogItems + systemItems
            val orders = normalized.backlogOrders + systemOrders
            val workspaceById = workspaces.associateBy { it.id }
            val workspaceIdsByContext =
                bundle.contexts.mapNotNull { context ->
                    val workspace = workspaceById[context.id]
                    val admitted =
                        if (
                            context.isDeleted &&
                            !SystemContexts.isSystem(ContextId(context.id))
                        ) {
                            workspace != null &&
                                workspace.id == context.id &&
                                workspace.isDeleted &&
                                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                                workspace.sourceContextId == null
                        } else {
                            isAdmittedLegacyContextIngressOwner(
                                contextId = context.id,
                                contextIsDeleted =
                                    if (SystemContexts.isSystem(ContextId(context.id))) null else true,
                                workspace = workspace,
                            )
                        }
                    if (admitted) context.id to requireNotNull(workspace).id else null
                }.toMap()
            val missingWorkspaceTargets =
                items.asSequence()
                    .filter { it.itemType == "PROJECT" || it.itemType == "SUBLIST" }
                    .map { it.entityId }
                    .filter { it.isNotBlank() && it !in contextsById }
                    .toSet()
            val targetStates = linkedMapOf<WorkspaceBacklogTargetRef, BacklogTargetState>()
            workspaces.forEach { workspace ->
                targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.WORKSPACE, workspace.id)] =
                    BacklogTargetState(workspace.isDeleted)
            }
            goalRows.forEach { rows ->
                if (rows.subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                    targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.ORIENTATION, rows.subject.id)] =
                        BacklogTargetState(rows.subject.isDeleted)
                }
            }
            bundle.linkItemEntities.forEach {
                targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.LINK_ITEM, it.id)] =
                    BacklogTargetState(it.isDeleted)
            }
            bundle.notes.forEach {
                targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.LEGACY_NOTE, it.id)] =
                    BacklogTargetState(it.isDeleted)
            }
            bundle.documents.forEach {
                targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, it.id)] =
                    BacklogTargetState(it.isDeleted)
            }
            bundle.checklists.forEach {
                targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.CHECKLIST, it.id)] =
                    BacklogTargetState(it.isDeleted)
            }
            bundle.musicNotes.forEach {
                targetStates[WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.MUSIC_NOTE, it.id)] =
                    BacklogTargetState(it.isDeleted)
            }
            val mappingByGoal = goalMappings.associate { it.sourceId to it.subjectId }
            val requiredGoalIds =
                items.asSequence()
                    .filter { it.itemType == "GOAL" }
                    .map { it.entityId }
                    .filter { it.isNotBlank() }
                    .toSet()
            require(requiredGoalIds.all { it in mappingByGoal }) {
                "Legacy BACKLOG references Goal identities missing from the backup: " +
                    (requiredGoalIds - mappingByGoal.keys).sorted()
            }
            val capabilityByWorkspace =
                workspaces.mapNotNull { workspace ->
                    capabilityByOwnerAndType[
                        workspace.id to WorkspaceCapabilityType.BACKLOG.name
                    ]?.let { capability ->
                        workspace.id to capability.id
                    }
                }.toMap()
            val plan =
                BacklogMigrationPlanner.plan(
                    items =
                        items.map {
                            LegacyBacklogItemSource(
                                id = it.id,
                                contextId = it.contextId,
                                itemType = it.itemType,
                                entityId = it.entityId,
                                associationOwnerContextId = it.associationOwnerContextId,
                                associationTag = it.associationTag,
                                order = it.order,
                                updatedAt = it.updatedAt,
                                syncedAt = it.syncedAt,
                                isDeleted = it.isDeleted,
                                version = it.version,
                            )
                        },
                    orders =
                        orders.map {
                            LegacyBacklogOrderSource(
                                id = it.id,
                                listId = it.listId,
                                itemId = it.itemId,
                                order = it.order,
                                orderVersion = it.orderVersion,
                                updatedAt = it.updatedAt,
                                syncedAt = it.syncedAt,
                                isDeleted = it.isDeleted,
                            )
                        },
                    bindings =
                        BacklogMigrationBindings(
                            workspaceIdByContextId = workspaceIdsByContext,
                            ownerWorkspaceStateById =
                                workspaceById.mapValues { BacklogOwnerWorkspaceState(it.value.isDeleted) },
                            capabilityInstanceIdByWorkspaceId = capabilityByWorkspace,
                            orientationIdByGoalId = mappingByGoal,
                            targetStateByRef = targetStates,
                            historicalMissingWorkspaceTargetContextIds = missingWorkspaceTargets,
                            historicalDeletedGoalIds =
                                bundle.goals.filter { it.isDeleted }.mapTo(hashSetOf()) { it.id },
                            parentWorkspaceIdByWorkspaceId =
                                workspaces.associate { it.id to it.parentWorkspaceId },
                        ),
                )
            require(plan.canApply && plan.isFullyAccounted) {
                buildString {
                    append("Restore legacy BACKLOG canonicalization was rejected")
                    plan.issues.forEach { append("\n${it.code}: ${it.detail}") }
                }
            }
            return plan.entries.map { entry ->
                WorkspaceBacklogEntrySnapshot(
                    id = entry.id,
                    workspaceId = entry.workspaceId,
                    capabilityInstanceId = entry.capabilityInstanceId,
                    targetKind = entry.target.kind.name,
                    targetId = entry.target.id,
                    order = entry.order,
                    createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt,
                    version = entry.version,
                    isDeleted = entry.isDeleted,
                )
            }
        }

        private fun canonicalizeLegacySystemApps(
            bundle: SnapshotBundle,
            liveWorkspaceIds: Set<String>,
        ) =
            bundle.systemApps.map { app ->
                val canonicalWorkspaceId =
                    app.workspaceId?.takeIf { it.isNotBlank() }
                val legacyWorkspaceId =
                    app.legacyContextId?.takeIf { it.isNotBlank() }

                when {
                    // Canonical ownership always wins. A contradictory legacy
                    // field is discarded rather than used as fallback.
                    canonicalWorkspaceId != null ->
                        app.copy(legacyContextId = null)

                    // Historical Context ownership is accepted only at this
                    // Restore boundary, and only when the same stable id has
                    // already become a live canonical Workspace.
                    legacyWorkspaceId != null &&
                        legacyWorkspaceId in liveWorkspaceIds ->
                        app.copy(
                            workspaceId = legacyWorkspaceId,
                            legacyContextId = null,
                        )

                    // Preserve the old restore behavior for unresolved owners:
                    // the canonical writer will reject/skip the ownerless row,
                    // but no legacy owner authority crosses this boundary.
                    else ->
                        app.copy(legacyContextId = null)
                }
            }

        private fun canonicalizeLegacyInboxSorting(
            bundle: SnapshotBundle,
            workspaces: List<WorkspaceEntity>,
            capabilities: List<WorkspaceCapabilityInstanceEntity>,
        ): List<WorkspaceCapabilityInstanceEntity> {
            if (bundle.contextInboxSortingRules.isEmpty()) return capabilities

            val workspaceById = workspaces.associateBy { it.id }
            val sourceContexts = bundle.contexts.associateBy { it.id }
            val workspaceIdByContextId =
                bundle.contextInboxSortingRules.mapNotNull { source ->
                    val workspace = workspaceById[source.contextId]
                    val context = sourceContexts[source.contextId]
                    val admitted =
                        isAdmittedLegacyContextIngressOwner(
                            contextId = source.contextId,
                            contextIsDeleted =
                                context
                                    ?.takeUnless { SystemContexts.isSystem(ContextId(it.id)) }
                                    ?.let { true },
                            workspace = workspace,
                        )
                    if (admitted) source.contextId to requireNotNull(workspace).id else null
                }.toMap()

            val capabilityInstanceIdByWorkspaceId =
                capabilities
                    .asSequence()
                    .filter {
                        !it.isDeleted &&
                            it.capabilityType == WorkspaceCapabilityType.INBOX_SORTING.name
                    }
                    .associate { it.workspaceId to it.id }

            val plan =
                InboxSortingLegacyPlanner.plan(
                    sources =
                        bundle.contextInboxSortingRules.map {
                            LegacyInboxSortingSource(
                                contextId = it.contextId,
                                rulesText = it.rulesText,
                                updatedAt = it.updatedAt,
                            )
                        },
                    bindings =
                        InboxSortingMigrationBindings(
                            workspaceIdByContextId = workspaceIdByContextId,
                            capabilityInstanceIdByWorkspaceId = capabilityInstanceIdByWorkspaceId,
                        ),
                )

            require(plan.canApply && plan.isFullyAccounted) {
                buildString {
                    append("Restore legacy INBOX_SORTING canonicalization was rejected")
                    plan.issues.forEach {
                        append("\n${it.code}: ${it.contextId}: ${it.detail}")
                    }
                }
            }

            val updatesByCapabilityId = plan.updates.associateBy { it.capabilityInstanceId }
            return capabilities.map { capability ->
                val update = updatesByCapabilityId[capability.id] ?: return@map capability
                capability.copy(
                    configurationVersion = update.configurationVersion,
                    configuration = update.configuration,
                    updatedAt = maxOf(capability.updatedAt, update.sourceUpdatedAt),
                    syncedAt = null,
                )
            }
        }

        private fun projectCapabilities(
            workspaces: List<WorkspaceEntity>,
            contextsById: Map<String, com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot>,
            configurationsByContextId: Map<String, com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextConfigurationSnapshot>,
            backlogOwnerIds: Set<String>,
            inboxOwnerIds: Set<String>,
            inboxSortingOwnerIds: Set<String>,
            now: Long,
        ): List<WorkspaceCapabilityInstanceEntity> {
            val resolver = ContextCapabilitiesResolver()
            val byLegacyId =
                orientationCapabilityRegistry
                    .flatMap { definition -> definition.legacyIds.map { it to definition.type } }
                    .toMap()
            return workspaces.flatMap { workspace ->
                val source = requireNotNull(contextsById[workspace.id])
                val rawConfig = configurationsByContextId[workspace.id]
                val config =
                    rawConfig?.let {
                        ContextConfiguration(
                            id = it.id,
                            contextId = it.contextId,
                            basePresetCode = it.basePresetCode,
                            experimentalCapabilityIds = it.experimentalCapabilityIds.orEmpty(),
                            applyMode = it.applyMode,
                            enableInbox = it.enableInbox,
                            enableLog = it.enableLog,
                            enableAdvanced = it.enableAdvanced,
                            enableDashboard = it.enableDashboard,
                            enableBacklog = it.enableBacklog,
                            enableAttachments = it.enableAttachments,
                            enableAutoLinkSubprojects = it.enableAutoLinkSubprojects,
                            removeInboxEntryAfterTagAutocopy = it.removeInboxEntryAfterTagAutocopy ?: false,
                            removeBacklogEntryAfterTagAutocopy = it.removeBacklogEntryAfterTagAutocopy ?: false,
                            version = it.version,
                            updatedAt = it.updatedAt,
                            isDeleted = false,
                        )
                    } ?: ContextConfiguration.default(workspace.id).copy(
                        basePresetCode = source.roleCode ?: "default",
                    )

                if (workspace.isDeleted) {
                    if (workspace.id !in backlogOwnerIds) return@flatMap emptyList()

                    val (configurationVersion, configuration) =
                        capabilityConfiguration(WorkspaceCapabilityType.BACKLOG, config)

                    return@flatMap listOf(
                        WorkspaceCapabilityInstanceEntity(
                            id = stableCapabilityId(workspace.id, WorkspaceCapabilityType.BACKLOG),
                            workspaceId = workspace.id,
                            capabilityType = WorkspaceCapabilityType.BACKLOG.name,
                            instanceKey = DEFAULT_INSTANCE_KEY,
                            capabilityOrder = WorkspaceCapabilityType.BACKLOG.ordinal.toLong(),
                            state = WorkspaceCapabilityState.DISABLED.name,
                            configurationVersion = configurationVersion,
                            configuration = configuration,
                            createdAt = workspace.createdAt,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = 1L,
                        ),
                    )
                }

                val mapped =
                    resolver.resolve(config)
                        .mapNotNull { byLegacyId[it.raw.trim().lowercase(Locale.ROOT)] }
                        .toMutableSet()
                if (workspace.id in inboxOwnerIds) mapped += WorkspaceCapabilityType.INBOX
                if (workspace.id in inboxSortingOwnerIds) {
                    mapped += WorkspaceCapabilityType.INBOX_SORTING
                }
                mapped += WorkspaceCapabilityType.DASHBOARD
                mapped += WorkspaceCapabilityType.EXECUTION_LOG
                mapped += WorkspaceCapabilityType.BACKLOG
                mapped.sortedBy { it.ordinal }.map { type ->
                    val active =
                        when (type) {
                            WorkspaceCapabilityType.DASHBOARD ->
                                resolver.resolve(config).any { it.raw.equals("dashboard", true) }
                            WorkspaceCapabilityType.EXECUTION_LOG ->
                                resolver.resolve(config).any { it.raw.equals("log", true) }
                            WorkspaceCapabilityType.BACKLOG ->
                                resolver.resolve(config).any { it.raw.equals("backlog", true) }
                            else -> true
                        }
                    val (configurationVersion, configuration) = capabilityConfiguration(type, config)
                    WorkspaceCapabilityInstanceEntity(
                        id = stableCapabilityId(workspace.id, type),
                        workspaceId = workspace.id,
                        capabilityType = type.name,
                        instanceKey = DEFAULT_INSTANCE_KEY,
                        capabilityOrder = type.ordinal.toLong(),
                        state =
                            if (active) WorkspaceCapabilityState.ACTIVE.name
                            else WorkspaceCapabilityState.DISABLED.name,
                        configurationVersion = configurationVersion,
                        configuration = configuration,
                        createdAt = workspace.createdAt,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                }
            }
        }

        private fun capabilityConfiguration(
            type: WorkspaceCapabilityType,
            config: ContextConfiguration,
        ): Pair<Int, String> =
            when (type) {
                WorkspaceCapabilityType.DASHBOARD ->
                    DashboardCapabilityConfigurationCodec.CURRENT_VERSION to
                        DashboardCapabilityConfigurationCodec.encodeDefault()
                WorkspaceCapabilityType.EXECUTION_LOG ->
                    ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION to
                        ExecutionLogCapabilityConfigurationCodec.encodeDefault()
                WorkspaceCapabilityType.BACKLOG ->
                    BacklogCapabilityConfigurationCodec.CURRENT_VERSION to
                        BacklogCapabilityConfigurationCodec.encode(
                            BacklogCapabilityConfigurationV2(
                                removeEntryAfterTagAutocopy =
                                    config.removeBacklogEntryAfterTagAutocopy == true,
                            ),
                        )
                WorkspaceCapabilityType.INBOX ->
                    InboxCapabilityConfigurationCodec.CURRENT_VERSION to
                        InboxCapabilityConfigurationCodec.encode(
                            InboxCapabilityConfigurationV1(
                                ownerVisibility =
                                    if (config.removeInboxEntryAfterTagAutocopy == true) {
                                        InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED
                                    } else {
                                        InboxOwnerVisibility.KEEP_VISIBLE
                                    },
                            ),
                        )
                WorkspaceCapabilityType.DIRECTION ->
                    DirectionCapabilityConfigurationCodec.CURRENT_VERSION to
                        DirectionCapabilityConfigurationCodec.encode(
                            DirectionCapabilityConfigurationV1(
                                autoLinkChildWorkspaces = config.enableAutoLinkSubprojects ?: true,
                            ),
                        )
                else -> 1 to "{}"
            }

        private fun stableCapabilityId(
            workspaceId: String,
            type: WorkspaceCapabilityType,
        ): String =
            LegacySubjectUuid.uuidV5(
                UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
                "WORKSPACE:CAPABILITY:$workspaceId:${type.name}:$DEFAULT_INSTANCE_KEY",
            ).toString()

        private fun requireNoLiveOrdinaryContexts(bundle: SnapshotBundle) {
            require(
                bundle.contexts.none { context ->
                    !context.isDeleted && !SystemContexts.isSystem(ContextId(context.id))
                },
            ) { "Canonical restore payload contains a live ordinary Context" }
        }

        private fun cycleMembers(parentById: Map<String, String?>): Set<String> {
            val result = mutableSetOf<String>()
            parentById.keys.forEach { start ->
                val path = mutableListOf<String>()
                val indexById = mutableMapOf<String, Int>()
                var current: String? = start
                while (current != null && current in parentById && current !in result) {
                    val repeatedAt = indexById[current]
                    if (repeatedAt != null) {
                        result += path.drop(repeatedAt)
                        break
                    }
                    indexById[current] = path.size
                    path += current
                    current = parentById[current]
                }
            }
            return result
        }

        private fun nextVersion(version: Long): Long =
            if (version == Long.MAX_VALUE) Long.MAX_VALUE else version + 1L

        private companion object {
            const val DEFAULT_INSTANCE_KEY = "default"
            const val GOAL_IDENTITY_CUTOVER_VERSION = 4
        }
    }
