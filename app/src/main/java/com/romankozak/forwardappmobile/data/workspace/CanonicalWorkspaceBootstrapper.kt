package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
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
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
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

/**
 * Maintains a read-only canonical shadow of Context identity and effective capabilities.
 * Context remains runtime/write authority until an explicit later cutover.
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

        private suspend fun refreshInCurrentTransaction(now: Long): WorkspaceBootstrapReport {
            val contexts = contextDao.getAll()
            val configurations = contextStructureDao.getAllSync().associateBy { it.contextId }
            val issues = mutableListOf<WorkspaceBootstrapIssueEntity>()
            val desiredWorkspaces = projectWorkspaces(contexts, issues, now)
            val existingWorkspaces = workspaceDao.getAll()
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
            val workspaceChanges = mergeWorkspaceProjection(existingWorkspaces, safeDesiredWorkspaces, issues, now)
            val contextBackedWorkspaceIds =
                existingWorkspaces
                    .filter { it.provenance == WorkspaceProvenance.CONTEXT_BACKED.name }
                    .mapTo(hashSetOf()) { it.id } +
                    desiredWorkspaceIds.filterNot { it in blockedContextIds }
            val capabilityChanges =
                projectCapabilityChanges(
                    contexts = contexts,
                    configurations = configurations,
                    existing = orientationDao.getAllWorkspaceCapabilities(),
                    contextBackedWorkspaceIds = contextBackedWorkspaceIds,
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
            issues: MutableList<WorkspaceBootstrapIssueEntity>,
            now: Long,
        ): List<WorkspaceEntity> {
            val ids = contexts.mapTo(hashSetOf()) { it.id }
            val parentById =
                contexts.associate { context ->
                    val parent = context.parentId?.takeIf { it in ids }
                    if (context.parentId != null && parent == null) {
                        issues += issue(context.id, "UNKNOWN_PARENT", "Missing Context parent ${context.parentId}", now)
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
                    it.id !in desiredIds
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
            contexts: List<Context>,
            configurations: Map<String, ContextConfiguration>,
            existing: List<WorkspaceCapabilityInstanceEntity>,
            contextBackedWorkspaceIds: Set<String>,
            blockedContextIds: Set<String>,
            issues: MutableList<WorkspaceBootstrapIssueEntity>,
            now: Long,
        ): List<WorkspaceCapabilityInstanceEntity> {
            val existingByLogical = existing.associateBy { Triple(it.workspaceId, it.capabilityType, it.instanceKey) }
            val desiredKeys = mutableSetOf<Triple<String, String, String>>()
            val changes = mutableListOf<WorkspaceCapabilityInstanceEntity>()
            contexts.filterNot { it.isDeleted || it.id in blockedContextIds }.forEach { context ->
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
                if (WorkspaceCapabilityType.INBOX_SORTING in mapped && WorkspaceCapabilityType.INBOX !in mapped) {
                    mapped -= WorkspaceCapabilityType.INBOX_SORTING
                    issues += issue(context.id, "MISSING_CAPABILITY_DEPENDENCY", "INBOX_SORTING requires INBOX", now)
                }
                mapped.sortedBy { capabilityOrder.getValue(it) }.forEach { type ->
                    val key = Triple(context.id, type.name, DEFAULT_INSTANCE_KEY)
                    desiredKeys += key
                    val current = existingByLogical[key]
                    val desired = desiredCapability(context, config, type, current, now)
                    if (current == null || !current.sameProjection(desired)) changes += desired
                }
            }
            existing.filter {
                it.instanceKey == DEFAULT_INSTANCE_KEY &&
                    !it.isDeleted &&
                    Triple(it.workspaceId, it.capabilityType, it.instanceKey) !in desiredKeys &&
                    it.workspaceId in contextBackedWorkspaceIds
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

        private fun desiredCapability(
            context: Context,
            contextConfiguration: ContextConfiguration,
            type: WorkspaceCapabilityType,
            current: WorkspaceCapabilityInstanceEntity?,
            now: Long,
        ): WorkspaceCapabilityInstanceEntity {
            val configurationVersion: Int
            val configuration: String
            if (type == WorkspaceCapabilityType.DIRECTION) {
                configurationVersion = DirectionCapabilityConfigurationCodec.CURRENT_VERSION
                configuration =
                    DirectionCapabilityConfigurationCodec.encode(
                        DirectionCapabilityConfigurationV1(
                            autoLinkChildWorkspaces =
                                contextConfiguration.enableAutoLinkSubprojects ?: true,
                        ),
                    )
            } else {
                configurationVersion = 1
                configuration = "{}"
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
