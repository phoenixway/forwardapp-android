package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.database.WorkspaceBootstrapIssueEntity
import com.romankozak.forwardappmobile.data.database.WorkspaceBootstrapStateEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class WorkspaceBootstrapReport(
    val projectedWorkspaces: Int,
    val projectedCapabilities: Int,
    val issues: List<WorkspaceBootstrapIssueEntity>,
    val performed: Boolean,
)

private data class CapabilityProjectionSource(
    val id: String,
    val roleCode: String?,
    val createdAt: Long,
    val isDeleted: Boolean,
)

/**
 * Maintains the Context-backed Workspace projection during incremental capability cutovers.
 * Context remains authority only for capabilities that have not crossed their explicit cutover boundary.
 */
@Singleton
class CanonicalWorkspaceBootstrapper
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val contextDao: ContextDao,
        private val contextStructureDao: ContextStructureDao,
    ) {
        private val mutex = Mutex()
        private val resolver = ContextCapabilitiesResolver()

        suspend fun ensureBootstrapped(now: Long = System.currentTimeMillis()): WorkspaceBootstrapReport =
            mutex.withLock {
                database.withTransaction {
                    refreshInCurrentTransaction(now)
                }
            }

        /**
         * Explicit compatibility ingress for a pre-canonical backup whose
         * workspaceCapabilityInstances field is absent.
         *
         * This is deliberately not used by startup or ordinary Context writes.
         * Only reserved System Context ids actually present in the imported
         * payload are eligible. Locally materialized compatibility shells are
         * not import evidence. Promoted System Workspaces may seed only genuinely
         * missing canonical instances; established canonical rows remain authoritative.
         */
        suspend fun ingestLegacySystemCapabilityProjection(
            importedContextIds: Set<String>,
            now: Long = System.currentTimeMillis(),
        ): WorkspaceBootstrapReport {
            val importedSystemContextIds =
                importedContextIds.filterTo(hashSetOf()) { id ->
                    SystemContexts.isSystem(ContextId(id))
                }
            val persistedById =
                contextDao.getAll()
                    .associateBy { it.id }
            val legacyContextEvidence =
                importedSystemContextIds.mapNotNull { id ->
                    persistedById[id]?.let { context ->
                        SystemWorkspaceLegacyContextEvidence(
                            id = context.id,
                            name = context.name,
                            description = context.description,
                            parentId = context.parentId,
                            roleCode = context.roleCode,
                            order = context.order,
                            createdAt = context.createdAt,
                            updatedAt = context.updatedAt,
                            isDeleted = context.isDeleted,
                            version = context.version,
                        )
                    }
                }

            return ingestLegacySystemCapabilityProjection(
                legacyContextEvidence = legacyContextEvidence,
                now = now,
            )
        }

        /**
         * Shell-free import-only compatibility boundary.
         *
         * Reserved System Context snapshots are transient evidence only. They
         * may seed genuinely missing canonical capability instances for an
         * already-live same-id canonical Workspace, but are never persisted as
         * Context rows and can never overwrite established canonical state.
         */
        suspend fun ingestLegacySystemCapabilityProjection(
            legacyContextEvidence: List<SystemWorkspaceLegacyContextEvidence>,
            legacyConfigurationEvidence: List<ContextConfiguration> = emptyList(),
            now: Long = System.currentTimeMillis(),
        ): WorkspaceBootstrapReport {
            val systemEvidenceById =
                legacyContextEvidence
                    .asSequence()
                    .filter { evidence ->
                        SystemContexts.isSystem(ContextId(evidence.id))
                    }
                    .associateBy { it.id }

            if (coroutineContext[WorkspaceMutationContext] != null) {
                return refreshInCurrentTransaction(
                    now = now,
                    promotedSystemLegacyCapabilityEvidence = systemEvidenceById,
                    promotedSystemLegacyConfigurationEvidence =
                        legacyConfigurationEvidence
                            .asSequence()
                            .filter { configuration ->
                                SystemContexts.isSystem(ContextId(configuration.contextId))
                            }
                            .associateBy { it.contextId },
                )
            }
            return mutex.withLock {
                database.withTransaction {
                    refreshInCurrentTransaction(
                        now = now,
                        promotedSystemLegacyCapabilityEvidence = systemEvidenceById,
                        promotedSystemLegacyConfigurationEvidence =
                            legacyConfigurationEvidence
                                .asSequence()
                                .filter { configuration ->
                                    SystemContexts.isSystem(ContextId(configuration.contextId))
                                }
                                .associateBy { it.contextId },
                    )
                }
            }
        }

        internal suspend fun <T> mutateAndRefresh(
            now: Long = System.currentTimeMillis(),
            mutation: suspend () -> T,
        ): T {
            if (coroutineContext[WorkspaceMutationContext] != null) return mutation()
            return mutex.withLock {
                withContext(WorkspaceMutationContext()) {
                    database.withTransaction {
                        val result = mutation()
                        refreshInCurrentTransaction(now)
                        result
                    }
                }
            }
        }

        internal suspend fun <T> mutateAndAfterRefresh(
            now: Long = System.currentTimeMillis(),
            mutation: suspend () -> T,
            afterRefresh: suspend (T) -> Unit,
        ): T {
            if (coroutineContext[WorkspaceMutationContext] != null) {
                val result = mutation()
                refreshInCurrentTransaction(now)
                afterRefresh(result)
                return result
            }
            return mutex.withLock {
                withContext(WorkspaceMutationContext()) {
                    database.withTransaction {
                        val result = mutation()
                        refreshInCurrentTransaction(now)
                        afterRefresh(result)
                        result
                    }
                }
            }
        }

        private suspend fun refreshInCurrentTransaction(
            now: Long,
            promotedSystemLegacyCapabilityEvidence:
                Map<String, SystemWorkspaceLegacyContextEvidence> = emptyMap(),
            promotedSystemLegacyConfigurationEvidence: Map<String, ContextConfiguration> = emptyMap(),
        ): WorkspaceBootstrapReport {
            val contexts = contextDao.getAll()
            val configurationRows = contextStructureDao.getAllSync()
            val persistedConfigurations =
                configurationRows
                    .filterNot { it.isDeleted }
                    .associateBy { it.contextId }
            val configurations =
                persistedConfigurations +
                    promotedSystemLegacyConfigurationEvidence.filterNot { it.value.isDeleted }
            val deletedConfigurationContextIds =
                configurationRows
                    .asSequence()
                    .filter { it.isDeleted }
                    .mapTo(hashSetOf()) { it.contextId }
                    .apply {
                        promotedSystemLegacyConfigurationEvidence
                            .filterValues { it.isDeleted }
                            .keys
                            .forEach(::add)
                    }
            val issues = mutableListOf<WorkspaceBootstrapIssueEntity>()
            val cutOverContextIds =
                orientationDao.getAllLegacyMappings()
                    .asSequence()
                    .filter {
                        !it.isDeleted &&
                            it.sourceType == LegacyOrientationSourceType.CONTEXT.name &&
                            it.state == LegacySubjectMappingState.CUT_OVER.name
                    }
                    .mapTo(hashSetOf()) { it.sourceId }
            val compatibilityContexts = contexts.filterNot { it.id in cutOverContextIds }
            val promotedSystemLegacyCapabilityIngressIds =
                promotedSystemLegacyCapabilityEvidence.keys
            val capabilityProjectionSourcesById =
                compatibilityContexts
                    .associate { context ->
                        context.id to
                            CapabilityProjectionSource(
                                id = context.id,
                                roleCode = context.roleCode,
                                createdAt = context.createdAt,
                                isDeleted = context.isDeleted,
                            )
                    }.toMutableMap()
            promotedSystemLegacyCapabilityEvidence.values.forEach { evidence ->
                capabilityProjectionSourcesById[evidence.id] =
                    CapabilityProjectionSource(
                        id = evidence.id,
                        roleCode = evidence.roleCode,
                        createdAt = evidence.createdAt,
                        isDeleted = evidence.isDeleted,
                    )
            }
            val capabilityProjectionSources = capabilityProjectionSourcesById.values.toList()
            val existingWorkspaces = workspaceDao.getAll()
            val canonicalCutOverWorkspaceIds =
                existingWorkspaces
                    .asSequence()
                    .filter {
                        !it.isDeleted &&
                            it.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                            it.id in cutOverContextIds
                    }
                    .mapTo(hashSetOf()) { it.id }

            /*
             * A live reserved System Context may temporarily coexist with its
             * same-id canonical Workspace after ownership cutover.
             *
             * Promoted System capability state is canonical. Ordinary startup,
             * refresh and Context writes never project ContextConfiguration
             * into it. Legacy capability input is accepted only when the
             * explicit pre-canonical-backup ingress requests it.
             */
            val promotedSystemWorkspaceIds =
                existingWorkspaces
                    .asSequence()
                    .filter {
                        !it.isDeleted &&
                            SystemContexts.isSystem(ContextId(it.id)) &&
                            it.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                            it.sourceContextId == null
                    }
                    .mapTo(hashSetOf()) { it.id }

            val metadataProjectionContexts =
                compatibilityContexts.filterNot { context ->
                    SystemContexts.isSystem(ContextId(context.id))
                }

            val desiredWorkspaces =
                projectWorkspaces(
                    contexts = metadataProjectionContexts,
                    canonicalCutOverWorkspaceIds =
                        canonicalCutOverWorkspaceIds + promotedSystemWorkspaceIds,
                    issues = issues,
                    now = now,
                )
            val desiredWorkspaceIds = desiredWorkspaces.mapTo(hashSetOf()) { it.id }
            val blockedContextIds =
                existingWorkspaces
                    .filter {
                        it.id in desiredWorkspaceIds &&
                            it.provenance != WorkspaceProvenance.CONTEXT_BACKED.name
                    }
                    .mapTo(hashSetOf()) { it.id }
            val safeDesiredWorkspaces =
                desiredWorkspaces.map { projected ->
                    val parentId = projected.parentWorkspaceId
                    if (
                        projected.id !in blockedContextIds &&
                        !SystemContexts.isSystem(ContextId(projected.id)) &&
                        parentId != null &&
                        parentId in blockedContextIds
                    ) {
                        issues +=
                            issue(
                                projected.id,
                                "WORKSPACE_PARENT_COLLISION",
                                "Context parent $parentId collides with a canonical Workspace; shadow parent was cleared",
                                now,
                            )
                        projected.copy(parentWorkspaceId = null)
                    } else {
                        projected
                    }
                }
            val workspaceChanges =
                mergeWorkspaceProjection(
                    existing = existingWorkspaces,
                    desired = safeDesiredWorkspaces,
                    issues = issues,
                    now = now,
                    protectedContextBackedIds = cutOverContextIds,
                )
            val legacyCapabilityProjectionWorkspaceIds =
                existingWorkspaces
                    .filter {
                        it.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                            it.id !in cutOverContextIds &&
                            !SystemContexts.isSystem(ContextId(it.id))
                    }
                    .mapTo(hashSetOf()) { it.id } +
                    desiredWorkspaceIds.filterNot { it in blockedContextIds } +
                    promotedSystemWorkspaceIds.intersect(
                        promotedSystemLegacyCapabilityIngressIds,
                    )
            val capabilityChanges =
                projectCapabilityChanges(
                    sources = capabilityProjectionSources,
                    configurations = configurations,
                    existing = orientationDao.getAllWorkspaceCapabilities(),
                    legacyCapabilityProjectionWorkspaceIds =
                        legacyCapabilityProjectionWorkspaceIds,
                    promotedSystemWorkspaceIds = promotedSystemWorkspaceIds,
                    promotedSystemLegacyCapabilityIngressIds =
                        promotedSystemLegacyCapabilityIngressIds,
                    deletedConfigurationContextIds = deletedConfigurationContextIds,
                    blockedContextIds = blockedContextIds,
                    issues = issues,
                    now = now,
                )

            if (workspaceChanges.isNotEmpty()) workspaceDao.upsert(workspaceChanges)
            if (capabilityChanges.isNotEmpty()) orientationDao.upsertWorkspaceCapabilities(capabilityChanges)
            workspaceDao.resolveOpenBootstrapIssues(now)
            if (issues.isNotEmpty()) workspaceDao.upsertBootstrapIssues(issues)
            workspaceDao.upsertBootstrapState(
                WorkspaceBootstrapStateEntity(
                    version = CURRENT_VERSION,
                    status = if (issues.isEmpty()) "COMPLETE" else "COMPLETE_WITH_ISSUES",
                    completedAt = now,
                    comparedAt = now,
                ),
            )
            return WorkspaceBootstrapReport(
                projectedWorkspaces = workspaceChanges.size,
                projectedCapabilities = capabilityChanges.size,
                issues = issues,
                performed = workspaceChanges.isNotEmpty() || capabilityChanges.isNotEmpty(),
            )
        }

        private fun projectWorkspaces(
            contexts: List<Context>,
            canonicalCutOverWorkspaceIds: Set<String>,
            issues: MutableList<WorkspaceBootstrapIssueEntity>,
            now: Long,
        ): List<WorkspaceEntity> {
            val ids = contexts.mapTo(hashSetOf()) { it.id }
            val parentById =
                contexts.associate { context ->
                    val parent =
                        context.parentId?.takeIf { parentId ->
                            parentId in ids ||
                                (
                                    SystemContexts.isSystem(ContextId(context.id)) &&
                                        parentId in canonicalCutOverWorkspaceIds
                                )
                        }
                    if (context.parentId != null && parent == null) {
                        issues += issue(
                            context.id,
                            "UNKNOWN_PARENT",
                            "Missing Context parent ${context.parentId}",
                            now,
                        )
                    }
                    context.id to parent
                }
            val cycleIds = cycleMembers(parentById)
            cycleIds.forEach { id ->
                issues += issue(id, "HIERARCHY_CYCLE", "Context hierarchy cycle was not projected", now)
            }
            return contexts.map { context ->
                WorkspaceEntity(
                    id = context.id,
                    nameOverride = context.name,
                    descriptionOverride = context.description,
                    parentWorkspaceId = parentById[context.id]?.takeUnless { context.id in cycleIds },
                    roleCode = context.roleCode,
                    workspaceOrder = context.order,
                    createdAt = context.createdAt,
                    updatedAt = context.updatedAt ?: context.createdAt,
                    syncedAt = null,
                    isDeleted = context.isDeleted,
                    version = context.version.coerceAtLeast(1L),
                    provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                    sourceContextId = context.id,
                )
            }
        }

        private fun mergeWorkspaceProjection(
            existing: List<WorkspaceEntity>,
            desired: List<WorkspaceEntity>,
            issues: MutableList<WorkspaceBootstrapIssueEntity>,
            now: Long,
            protectedContextBackedIds: Set<String> = emptySet(),
        ): List<WorkspaceEntity> {
            val existingById = existing.associateBy { it.id }
            val desiredIds = desired.mapTo(hashSetOf()) { it.id }
            val changes =
                desired.mapNotNull { projected ->
                    val current = existingById[projected.id] ?: return@mapNotNull projected.copy(updatedAt = now)
                    if (current.provenance != WorkspaceProvenance.CONTEXT_BACKED.name) {
                        issues +=
                            issue(
                                projected.id,
                                "WORKSPACE_ID_COLLISION",
                                "Context id collides with ${current.provenance} Workspace and was not projected",
                                now,
                            )
                        return@mapNotNull null
                    }
                    if (current.sameProjection(projected)) null
                    else projected.copy(
                        createdAt = current.createdAt,
                        updatedAt = now,
                        version = current.version + 1L,
                    )
                }.toMutableList()

            existing.filter {
                !it.isDeleted &&
                    it.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                    it.id !in desiredIds &&
                    it.id !in protectedContextBackedIds
            }.forEach {
                changes += it.copy(
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = true,
                    version = it.version + 1L,
                )
            }
            return changes
        }

        private fun projectCapabilityChanges(
            sources: List<CapabilityProjectionSource>,
            configurations: Map<String, ContextConfiguration>,
            existing: List<WorkspaceCapabilityInstanceEntity>,
            legacyCapabilityProjectionWorkspaceIds: Set<String>,
            promotedSystemWorkspaceIds: Set<String>,
            promotedSystemLegacyCapabilityIngressIds: Set<String>,
            deletedConfigurationContextIds: Set<String>,
            blockedContextIds: Set<String>,
            issues: MutableList<WorkspaceBootstrapIssueEntity>,
            now: Long,
        ): List<WorkspaceCapabilityInstanceEntity> {
            val existingByLogical = existing.associateBy { Triple(it.workspaceId, it.capabilityType, it.instanceKey) }
            val desiredKeys = mutableSetOf<Triple<String, String, String>>()
            val changes = mutableListOf<WorkspaceCapabilityInstanceEntity>()

            // Canonical System ownership is established by the live promoted
            // Workspace, not by the lifecycle of its compatibility Context.
            // Any existing logical row is established state, including a
            // disabled, archived, or deleted instance, and must survive generic
            // legacy cleanup unchanged. A missing row is intentionally not
            // seeded from a deleted/absent compatibility Context below.
            promotedSystemWorkspaceIds.forEach { workspaceId ->
                listOf(
                    WorkspaceCapabilityType.INBOX,
                    WorkspaceCapabilityType.CONNECTIONS,
                    WorkspaceCapabilityType.DIRECTION,
                    WorkspaceCapabilityType.INBOX_SORTING,
                    WorkspaceCapabilityType.KEY_PROBLEMS,
                ).forEach { type ->
                    val key = Triple(workspaceId, type.name, DEFAULT_INSTANCE_KEY)
                    if (existingByLogical[key] != null) desiredKeys += key
                }
            }
            // A tombstoned compatibility configuration is an explicit absence
            // of live seed input, not a request to apply default capability
            // values or clean up already-persisted canonical state.
            deletedConfigurationContextIds.forEach { contextId ->
                existing.asSequence()
                    .filter {
                        it.workspaceId == contextId &&
                            it.instanceKey == DEFAULT_INSTANCE_KEY
                    }
                    .forEach {
                        desiredKeys += Triple(it.workspaceId, it.capabilityType, it.instanceKey)
                    }
            }
            sources.filterNot { it.isDeleted || it.id in blockedContextIds }.forEach { context ->
                if (context.id in deletedConfigurationContextIds) return@forEach

                val isReservedSystem = SystemContexts.isSystem(ContextId(context.id))
                if (isReservedSystem) {
                    // Normal bootstrap never derives canonical System capability
                    // state from compatibility ContextConfiguration. The only
                    // allowed legacy ingress is an explicit pre-canonical backup,
                    // and it requires an already-live canonical same-id Workspace.
                    if (context.id !in promotedSystemLegacyCapabilityIngressIds) {
                        return@forEach
                    }
                    require(context.id in promotedSystemWorkspaceIds) {
                        "Legacy System capability ingress requires canonical Workspace: ${context.id}"
                    }
                }
                val config =
                    configurations[context.id]?.let {
                        if (it.basePresetCode == null) it.copy(basePresetCode = context.roleCode) else it
                    } ?: ContextConfiguration.default(context.id).copy(
                        basePresetCode = context.roleCode ?: "default",
                    )
                val resolved = resolver.resolve(config).map { it.raw.trim().lowercase(Locale.ROOT) }.toSet()
                val mapped = mutableSetOf<WorkspaceCapabilityType>()
                val unknown = mutableListOf<String>()
                resolved.forEach { raw ->
                    val type = capabilityByLegacyId[raw]
                    if (type == null) {
                        unknown += raw
                    } else {
                        mapped += type
                    }
                }
                if (unknown.isNotEmpty()) {
                    issues +=
                        issue(
                            context.id,
                            "UNKNOWN_CAPABILITY",
                            "Capabilities ${unknown.sorted()} were preserved only in Context",
                            now,
                        )
                }
                seedDashboardAfterCutover(
                    context = context,
                    mapped = mapped,
                    existingByLogical = existingByLogical,
                    desiredKeys = desiredKeys,
                    changes = changes,
                    now = now,
                )
                seedExecutionLogAfterCutover(
                    context = context,
                    mapped = mapped,
                    existingByLogical = existingByLogical,
                    desiredKeys = desiredKeys,
                    changes = changes,
                    now = now,
                )
                seedBacklogAfterCutover(
                    context = context,
                    contextConfiguration = config,
                    isPromotedSystem = context.id in promotedSystemWorkspaceIds,
                    mapped = mapped,
                    existingByLogical = existingByLogical,
                    desiredKeys = desiredKeys,
                    changes = changes,
                    now = now,
                )
                mapped -= WorkspaceCapabilityType.DASHBOARD
                mapped -= WorkspaceCapabilityType.EXECUTION_LOG
                mapped -= WorkspaceCapabilityType.BACKLOG
                if (context.id in promotedSystemWorkspaceIds) {
                    preserveExistingCanonicalSystemCapability(
                        contextId = context.id,
                        type = WorkspaceCapabilityType.INBOX,
                        mapped = mapped,
                        existingByLogical = existingByLogical,
                        desiredKeys = desiredKeys,
                    )
                    preserveExistingCanonicalSystemCapability(
                        contextId = context.id,
                        type = WorkspaceCapabilityType.DIRECTION,
                        mapped = mapped,
                        existingByLogical = existingByLogical,
                        desiredKeys = desiredKeys,
                    )
                    preserveExistingCanonicalSystemCapability(
                        contextId = context.id,
                        type = WorkspaceCapabilityType.CONNECTIONS,
                        mapped = mapped,
                        existingByLogical = existingByLogical,
                        desiredKeys = desiredKeys,
                    )
                    preserveExistingCanonicalSystemCapability(
                        contextId = context.id,
                        type = WorkspaceCapabilityType.INBOX_SORTING,
                        mapped = mapped,
                        existingByLogical = existingByLogical,
                        desiredKeys = desiredKeys,
                    )
                    preserveExistingCanonicalSystemCapability(
                        contextId = context.id,
                        type = WorkspaceCapabilityType.KEY_PROBLEMS,
                        mapped = mapped,
                        existingByLogical = existingByLogical,
                        desiredKeys = desiredKeys,
                    )
                }
                mapped.sortedBy { capabilityOrder.getValue(it) }.forEach { type ->
                    val key = Triple(context.id, type.name, DEFAULT_INSTANCE_KEY)
                    desiredKeys += key
                    val current = existingByLogical[key]
                    val desired = desiredCapability(context, config, type, current, now)
                    if (current == null || !current.sameProjection(desired)) changes += desired
                }
            }
            val liveContextBackedOwnerIds =
                sources
                    .filterNot { it.isDeleted || it.id in blockedContextIds }
                    .mapTo(hashSetOf()) { it.id }

            existing.filter {
                it.instanceKey == DEFAULT_INSTANCE_KEY &&
                    it.capabilityType == WorkspaceCapabilityType.EXECUTION_LOG.name &&
                    !it.isDeleted &&
                    it.workspaceId in legacyCapabilityProjectionWorkspaceIds &&
                    it.workspaceId !in liveContextBackedOwnerIds
            }.forEach { current ->
                changes +=
                    current.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = true,
                        version = current.version + 1L,
                    )
            }

            existing.filter {
                it.instanceKey == DEFAULT_INSTANCE_KEY &&
                    it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name &&
                    !it.isDeleted &&
                    it.workspaceId in legacyCapabilityProjectionWorkspaceIds &&
                    it.workspaceId !in liveContextBackedOwnerIds
            }.forEach { current ->
                changes +=
                    current.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = true,
                        version = current.version + 1L,
                    )
            }

            existing.filter {
                it.instanceKey == DEFAULT_INSTANCE_KEY &&
                    it.capabilityType != WorkspaceCapabilityType.DASHBOARD.name &&
                    it.capabilityType != WorkspaceCapabilityType.EXECUTION_LOG.name &&
                    !it.isDeleted &&
                    Triple(it.workspaceId, it.capabilityType, it.instanceKey) !in desiredKeys &&
                    it.workspaceId in legacyCapabilityProjectionWorkspaceIds
            }.forEach { current ->
                changes +=
                    current.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = true,
                        version = current.version + 1L,
                    )
            }
            return changes
        }

        /**
         * Import-only compatibility: a pre-canonical backup may seed a missing
         * promoted-System instance. Established canonical state always wins.
         */
        private fun preserveExistingCanonicalSystemCapability(
            contextId: String,
            type: WorkspaceCapabilityType,
            mapped: MutableSet<WorkspaceCapabilityType>,
            existingByLogical: Map<Triple<String, String, String>, WorkspaceCapabilityInstanceEntity>,
            desiredKeys: MutableSet<Triple<String, String, String>>,
        ) {
            val key = Triple(contextId, type.name, DEFAULT_INSTANCE_KEY)
            if (existingByLogical[key] == null) return
            desiredKeys += key
            mapped -= type
        }

        private fun seedDashboardAfterCutover(
            context: CapabilityProjectionSource,
            mapped: Set<WorkspaceCapabilityType>,
            existingByLogical: Map<Triple<String, String, String>, WorkspaceCapabilityInstanceEntity>,
            desiredKeys: MutableSet<Triple<String, String, String>>,
            changes: MutableList<WorkspaceCapabilityInstanceEntity>,
            now: Long,
        ) {
            val key = Triple(context.id, WorkspaceCapabilityType.DASHBOARD.name, DEFAULT_INSTANCE_KEY)
            val current = existingByLogical[key]
            if (current != null) {
                desiredKeys += key
                return
            }

            desiredKeys += key
            changes +=
                WorkspaceCapabilityInstanceEntity(
                    id = stableId("CAPABILITY:${context.id}:${WorkspaceCapabilityType.DASHBOARD.name}:$DEFAULT_INSTANCE_KEY"),
                    workspaceId = context.id,
                    capabilityType = WorkspaceCapabilityType.DASHBOARD.name,
                    instanceKey = DEFAULT_INSTANCE_KEY,
                    capabilityOrder = capabilityOrder.getValue(WorkspaceCapabilityType.DASHBOARD).toLong(),
                    state =
                        if (WorkspaceCapabilityType.DASHBOARD in mapped) {
                            WorkspaceCapabilityState.ACTIVE.name
                        } else {
                            WorkspaceCapabilityState.DISABLED.name
                        },
                    configurationVersion = DashboardCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration = DashboardCapabilityConfigurationCodec.encodeDefault(),
                    createdAt = context.createdAt,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                )
        }

        private fun seedExecutionLogAfterCutover(
            context: CapabilityProjectionSource,
            mapped: Set<WorkspaceCapabilityType>,
            existingByLogical: Map<Triple<String, String, String>, WorkspaceCapabilityInstanceEntity>,
            desiredKeys: MutableSet<Triple<String, String, String>>,
            changes: MutableList<WorkspaceCapabilityInstanceEntity>,
            now: Long,
        ) {
            val key = Triple(context.id, WorkspaceCapabilityType.EXECUTION_LOG.name, DEFAULT_INSTANCE_KEY)
            val current = existingByLogical[key]

            if (current != null) {
                desiredKeys += key
                return
            }

            desiredKeys += key
            changes +=
                WorkspaceCapabilityInstanceEntity(
                    id =
                        stableId(
                            "CAPABILITY:${context.id}:${WorkspaceCapabilityType.EXECUTION_LOG.name}:$DEFAULT_INSTANCE_KEY",
                        ),
                    workspaceId = context.id,
                    capabilityType = WorkspaceCapabilityType.EXECUTION_LOG.name,
                    instanceKey = DEFAULT_INSTANCE_KEY,
                    capabilityOrder = capabilityOrder.getValue(WorkspaceCapabilityType.EXECUTION_LOG).toLong(),
                    state =
                        if (WorkspaceCapabilityType.EXECUTION_LOG in mapped) {
                            WorkspaceCapabilityState.ACTIVE.name
                        } else {
                            WorkspaceCapabilityState.DISABLED.name
                        },
                    configurationVersion = ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration = ExecutionLogCapabilityConfigurationCodec.encodeDefault(),
                    createdAt = context.createdAt,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                )
        }

        private fun seedBacklogAfterCutover(
            context: CapabilityProjectionSource,
            contextConfiguration: ContextConfiguration,
            isPromotedSystem: Boolean,
            mapped: Set<WorkspaceCapabilityType>,
            existingByLogical: Map<Triple<String, String, String>, WorkspaceCapabilityInstanceEntity>,
            desiredKeys: MutableSet<Triple<String, String, String>>,
            changes: MutableList<WorkspaceCapabilityInstanceEntity>,
            now: Long,
        ) {
            val key = Triple(context.id, WorkspaceCapabilityType.BACKLOG.name, DEFAULT_INSTANCE_KEY)
            val current = existingByLogical[key]
            val desiredOrder = capabilityOrder.getValue(WorkspaceCapabilityType.BACKLOG).toLong()
            val promotedSystemConfiguration =
                BacklogCapabilityConfigurationV2(
                    removeEntryAfterTagAutocopy =
                        contextConfiguration.removeBacklogEntryAfterTagAutocopy == true,
                )

            if (current != null) {
                desiredKeys += key
                if (isPromotedSystem && current.configurationVersion == 1) {
                    val validV1 =
                        runCatching {
                            BacklogCapabilityConfigurationCodec.validate(
                                current.configurationVersion,
                                current.configuration,
                            )
                        }.isSuccess
                    if (validV1) {
                        changes +=
                            current.copy(
                                configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                                configuration = BacklogCapabilityConfigurationCodec.encode(promotedSystemConfiguration),
                                updatedAt = now,
                                syncedAt = null,
                                version = current.version + 1L,
                            )
                    }
                    return
                }
                if (isPromotedSystem) {
                    // Existing v2, unsupported, and malformed canonical state is
                    // established authority. Legacy configuration cannot rewrite it.
                    return
                }
                if (current.capabilityOrder != desiredOrder) {
                    changes +=
                        current.copy(
                            capabilityOrder = desiredOrder,
                            updatedAt = now,
                            syncedAt = null,
                            version = current.version + 1L,
                        )
                }
                return
            }

            desiredKeys += key
            changes +=
                WorkspaceCapabilityInstanceEntity(
                    id = stableId("CAPABILITY:${context.id}:${WorkspaceCapabilityType.BACKLOG.name}:$DEFAULT_INSTANCE_KEY"),
                    workspaceId = context.id,
                    capabilityType = WorkspaceCapabilityType.BACKLOG.name,
                    instanceKey = DEFAULT_INSTANCE_KEY,
                    capabilityOrder = desiredOrder,
                    state =
                        if (WorkspaceCapabilityType.BACKLOG in mapped) {
                            WorkspaceCapabilityState.ACTIVE.name
                        } else {
                            WorkspaceCapabilityState.DISABLED.name
                        },
                    configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration =
                        if (isPromotedSystem) {
                            BacklogCapabilityConfigurationCodec.encode(promotedSystemConfiguration)
                        } else {
                            BacklogCapabilityConfigurationCodec.encodeDefault()
                        },
                    createdAt = context.createdAt,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                )
        }

        private fun desiredCapability(
            context: CapabilityProjectionSource,
            contextConfiguration: ContextConfiguration,
            type: WorkspaceCapabilityType,
            current: WorkspaceCapabilityInstanceEntity?,
            now: Long,
        ): WorkspaceCapabilityInstanceEntity {
            val configurationVersion: Int
            val configuration: String
            when (type) {
                WorkspaceCapabilityType.DIRECTION -> {
                    configurationVersion = DirectionCapabilityConfigurationCodec.CURRENT_VERSION
                    configuration =
                        DirectionCapabilityConfigurationCodec.encode(
                            DirectionCapabilityConfigurationV1(
                                autoLinkChildWorkspaces =
                                    contextConfiguration.enableAutoLinkSubprojects ?: true,
                            ),
                        )
                }

                WorkspaceCapabilityType.INBOX -> {
                    configurationVersion = InboxCapabilityConfigurationCodec.CURRENT_VERSION
                    configuration =
                        InboxCapabilityConfigurationCodec.encode(
                            InboxCapabilityConfigurationV1(
                                ownerVisibility =
                                    if (contextConfiguration.removeInboxEntryAfterTagAutocopy == true) {
                                        InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED
                                    } else {
                                        InboxOwnerVisibility.KEEP_VISIBLE
                                    },
                            ),
                        )
                }

                WorkspaceCapabilityType.INBOX_SORTING -> {
                    configurationVersion =
                        current?.configurationVersion
                            ?: InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION
                    configuration =
                        current?.configuration
                            ?: InboxSortingCapabilityConfigurationCodec.encodeDefault()
                }

                else -> {
                    configurationVersion = 1
                    configuration = "{}"
                }
            }
            return WorkspaceCapabilityInstanceEntity(
                id = current?.id ?: stableId("CAPABILITY:${context.id}:${type.name}:$DEFAULT_INSTANCE_KEY"),
                workspaceId = context.id,
                capabilityType = type.name,
                instanceKey = DEFAULT_INSTANCE_KEY,
                capabilityOrder = capabilityOrder.getValue(type).toLong(),
                state = WorkspaceCapabilityState.ACTIVE.name,
                configurationVersion = configurationVersion,
                configuration = configuration,
                createdAt = current?.createdAt ?: context.createdAt,
                updatedAt = now,
                syncedAt = null,
                isDeleted = false,
                version = (current?.version ?: 0L) + 1L,
            )
        }

        private fun issue(contextId: String, code: String, detail: String, now: Long) =
            WorkspaceBootstrapIssueEntity(
                id = stableId("ISSUE:$contextId:$code"),
                contextId = contextId,
                code = code,
                detail = detail,
                createdAt = now,
                resolvedAt = null,
            )

        private fun stableId(name: String): String = LegacySubjectUuid.uuidV5(namespace, "WORKSPACE:$name").toString()

        companion object {
            const val CURRENT_VERSION = 1
            private const val DEFAULT_INSTANCE_KEY = "default"
            private val namespace = UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID)
            private val capabilityOrder = WorkspaceCapabilityType.entries.withIndex().associate { it.value to it.index }
            private val capabilityByLegacyId =
                orientationCapabilityRegistry
                    .flatMap { definition -> definition.legacyIds.map { it to definition.type } }
                    .toMap()
        }
    }

private class WorkspaceMutationContext :
    AbstractCoroutineContextElement(WorkspaceMutationContext) {
    companion object Key : CoroutineContext.Key<WorkspaceMutationContext>
}

private fun WorkspaceEntity.sameProjection(other: WorkspaceEntity): Boolean =
    nameOverride == other.nameOverride &&
        descriptionOverride == other.descriptionOverride &&
        parentWorkspaceId == other.parentWorkspaceId &&
        roleCode == other.roleCode &&
        workspaceOrder == other.workspaceOrder &&
        provenance == other.provenance &&
        sourceContextId == other.sourceContextId &&
        isDeleted == other.isDeleted

private fun WorkspaceCapabilityInstanceEntity.sameProjection(other: WorkspaceCapabilityInstanceEntity): Boolean =
    workspaceId == other.workspaceId &&
        capabilityType == other.capabilityType &&
        instanceKey == other.instanceKey &&
        capabilityOrder == other.capabilityOrder &&
        state == other.state &&
        configurationVersion == other.configurationVersion &&
        configuration == other.configuration &&
        isDeleted == other.isDeleted

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
