package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinition
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** 
 * Transient pre-canonical evidence carried by old Context-shaped payloads.
 *
 * This type deliberately is not a Room entity and must never be persisted as a
 * reserved Context shell. It exists only so old backup/merge payloads can
 * converge canonical same-id System Workspace ownership.
 */
data class SystemWorkspaceLegacyContextEvidence(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val roleCode: String?,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

data class SystemWorkspaceMaterializationResult(
    val created: Int,
    val preservedCanonical: Int,
    val promotedLegacy: Int,
    val seededFactoryCapabilities: Int,
) {
    val changed: Boolean =
        created > 0 ||
            promotedLegacy > 0 ||
            seededFactoryCapabilities > 0
}

/**
 * Canonical factory owner for fixed-id reserved System Workspaces.
 *
 * Factory definitions are create-time defaults only. Existing canonical rows
 * are never synchronized from them. Historical reserved Context rows are
 * bounded migration evidence only: a missing same-id Workspace is created
 * directly as CANONICAL_ONLY from that metadata, while an old CONTEXT_BACKED
 * projection is validated and promoted in place.
 */
@Singleton
class SystemWorkspaceMaterializer
    @Inject
    constructor(
        private val database: AppDatabase,
        private val contextDao: ContextDao,
        private val workspaceDao: WorkspaceDao,
    ) {
        suspend fun materializeAll(
            now: Long = System.currentTimeMillis(),
            seedMissingFactoryCapabilities: Boolean = true,
            legacyContextEvidence: Collection<SystemWorkspaceLegacyContextEvidence> = emptyList(),
        ): SystemWorkspaceMaterializationResult =
            database.withTransaction {
                val definitions = orderedDefinitions()
                val importedLegacyEvidence = linkedMapOf<String, SystemWorkspaceLegacyContextEvidence>()
                legacyContextEvidence.forEach { evidence ->
                    require(SystemContexts.isSystem(ContextId(evidence.id))) {
                        "Legacy System Workspace evidence is not reserved: ${evidence.id}"
                    }
                    require(importedLegacyEvidence.put(evidence.id, evidence) == null) {
                        "Duplicate legacy System Workspace evidence: ${evidence.id}"
                    }
                }

                val contexts =
                    contextDao.getAll()
                        .associateBy { it.id }
                        .mapValues { (_, context) -> context.toLegacySystemWorkspaceEvidence() }
                val workspaces = workspaceDao.getAll().associateBy { it.id }
                val toCreate = mutableListOf<WorkspaceEntity>()
                val toPromote = mutableListOf<WorkspaceEntity>()
                var preservedCanonical = 0

                definitions.forEach { definition ->
                    val workspace = workspaces[definition.id]
                    // Imported evidence has precedence over a surviving local
                    // shell because before Step 11 ingress retirement the
                    // incoming Context row was persisted first and therefore
                    // was the evidence observed by this materializer.
                    val legacyContext =
                        importedLegacyEvidence[definition.id]
                            ?: contexts[definition.id]

                    if (workspace == null) {
                        if (legacyContext?.isDeleted == true) {
                            error("Reserved System Context is deleted: ${definition.id}")
                        }

                        val created =
                            if (legacyContext == null) {
                                definition.toWorkspace(now)
                            } else {
                                legacyContext.toCanonicalSystemWorkspace()
                            }
                        toCreate += created
                        return@forEach
                    }

                    require(!workspace.isDeleted) {
                        "Reserved System Workspace is deleted: ${definition.id}"
                    }

                    when {
                        workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                            workspace.sourceContextId == null -> {
                            preservedCanonical += 1
                        }

                        workspace.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                            workspace.sourceContextId == definition.id -> {
                            val context =
                                requireNotNull(legacyContext) {
                                    "Context-backed System Workspace has no live same-id Context: ${definition.id}"
                                }
                            require(!context.isDeleted) {
                                "Context-backed System Workspace has deleted same-id Context: ${definition.id}"
                            }

                            val staleFields =
                                buildList {
                                    if (workspace.nameOverride != context.name) add("name")
                                    if (workspace.descriptionOverride != context.description) add("description")
                                    if (workspace.parentWorkspaceId != context.parentId) add("parent")
                                    if (workspace.roleCode != context.roleCode) add("role")
                                    if (workspace.workspaceOrder != context.order) add("order")
                                }
                            require(staleFields.isEmpty()) {
                                "System Workspace projection is stale for ${definition.id}: " +
                                    staleFields.joinToString()
                            }

                            toPromote +=
                                workspace.copy(
                                    updatedAt = now,
                                    syncedAt = null,
                                    version = nextVersion(workspace.version),
                                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                                    sourceContextId = null,
                                )
                        }

                        else ->
                            error(
                                "Unexpected System Workspace ownership state for ${definition.id}: " +
                                    "provenance=${workspace.provenance}, " +
                                    "sourceContextId=${workspace.sourceContextId}",
                            )
                    }
                }

                val resultingWorkspaceIds =
                    workspaces.keys + toCreate.map { it.id } + toPromote.map { it.id }
                (toCreate + toPromote).forEach { workspace ->
                    val parentId = workspace.parentWorkspaceId
                    require(parentId == null || parentId in resultingWorkspaceIds) {
                        "Reserved System Workspace has no live parent: " +
                            "${workspace.id} -> $parentId"
                    }
                }

                if (toCreate.isNotEmpty() || toPromote.isNotEmpty()) {
                    workspaceDao.upsert(toCreate + toPromote)
                }

                /*
                 * Factory capability defaults are a second phase from ownership.
                 *
                 * A pre-canonical backup must first get canonical same-id
                 * Workspace owners, then its explicit legacy capability ingress
                 * must have the first opportunity to create logical instances.
                 * Only afterwards may defaults fill genuinely absent logical
                 * instances.
                 *
                 * Any existing row, including disabled/deleted/tombstoned state,
                 * is established canonical authority and is never resurrected or
                 * overwritten by factory defaults.
                 */
                val seededFactoryCapabilities =
                    if (seedMissingFactoryCapabilities) {
                        val existingLogicalKeys =
                            database.orientationDao()
                                .getAllWorkspaceCapabilities()
                                .mapTo(hashSetOf()) { capability ->
                                    Triple(
                                        capability.workspaceId,
                                        capability.capabilityType,
                                        capability.instanceKey,
                                    )
                                }

                        val missingFactoryCapabilities =
                            definitions.flatMap { definition ->
                                definition.toFactoryCapabilities(now).filter { capability ->
                                    Triple(
                                        capability.workspaceId,
                                        capability.capabilityType,
                                        capability.instanceKey,
                                    ) !in existingLogicalKeys
                                }
                            }

                        if (missingFactoryCapabilities.isNotEmpty()) {
                            database.orientationDao().upsertWorkspaceCapabilities(
                                missingFactoryCapabilities,
                            )
                        }
                        missingFactoryCapabilities.size
                    } else {
                        0
                    }

                SystemWorkspaceMaterializationResult(
                    created = toCreate.size,
                    preservedCanonical = preservedCanonical,
                    promotedLegacy = toPromote.size,
                    seededFactoryCapabilities = seededFactoryCapabilities,
                )
            }

        private fun orderedDefinitions(): List<SystemOperationalDefinition> {
            val definitions = SystemOperationalDefinitions.all
            val byId = definitions.associateBy { it.id }
            require(byId.size == definitions.size) {
                "System operational definitions contain duplicate ids"
            }
            definitions.forEach { definition ->
                require(SystemContexts.isSystem(ContextId(definition.id))) {
                    "System operational definition is not reserved: ${definition.id}"
                }
                require(definition.defaultParentId == null || definition.defaultParentId in byId) {
                    "Unknown System Workspace factory parent for ${definition.id}: " +
                        definition.defaultParentId
                }
            }

            val ordered = mutableListOf<SystemOperationalDefinition>()
            val visiting = hashSetOf<String>()
            val visited = hashSetOf<String>()

            fun visit(definition: SystemOperationalDefinition) {
                if (definition.id in visited) return
                check(visiting.add(definition.id)) {
                    "System Workspace factory hierarchy contains a cycle at ${definition.id}"
                }
                definition.defaultParentId?.let { parentId ->
                    visit(requireNotNull(byId[parentId]))
                }
                visiting.remove(definition.id)
                visited += definition.id
                ordered += definition
            }

            definitions.forEach(::visit)
            return ordered
        }

        /**
         * Fresh canonical System owners preserve the exact pre-cutover factory
         * state, but without consulting ContextConfiguration:
         *
         * DASHBOARD     ACTIVE
         * EXECUTION_LOG DISABLED
         * BACKLOG       DISABLED, configuration v2 with remove=false
         *
         * The other five TARGET capabilities remain absent until explicitly
         * authored. These are create-time defaults only.
         */
        private fun SystemOperationalDefinition.toFactoryCapabilities(
            now: Long,
        ): List<WorkspaceCapabilityInstanceEntity> =
            listOf(
                factoryCapability(
                    workspaceId = id,
                    type = WorkspaceCapabilityType.DASHBOARD,
                    state = WorkspaceCapabilityState.ACTIVE,
                    configurationVersion = DashboardCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration = DashboardCapabilityConfigurationCodec.encodeDefault(),
                    now = now,
                ),
                factoryCapability(
                    workspaceId = id,
                    type = WorkspaceCapabilityType.EXECUTION_LOG,
                    state = WorkspaceCapabilityState.DISABLED,
                    configurationVersion = ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration = ExecutionLogCapabilityConfigurationCodec.encodeDefault(),
                    now = now,
                ),
                factoryCapability(
                    workspaceId = id,
                    type = WorkspaceCapabilityType.BACKLOG,
                    state = WorkspaceCapabilityState.DISABLED,
                    configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration =
                        BacklogCapabilityConfigurationCodec.encode(
                            BacklogCapabilityConfigurationV2(
                                removeEntryAfterTagAutocopy = false,
                            ),
                        ),
                    now = now,
                ),
            )

        private fun factoryCapability(
            workspaceId: String,
            type: WorkspaceCapabilityType,
            state: WorkspaceCapabilityState,
            configurationVersion: Int,
            configuration: String,
            now: Long,
        ): WorkspaceCapabilityInstanceEntity =
            WorkspaceCapabilityInstanceEntity(
                id = stableCapabilityId(workspaceId, type),
                workspaceId = workspaceId,
                capabilityType = type.name,
                instanceKey = DEFAULT_INSTANCE_KEY,
                capabilityOrder = capabilityOrder.getValue(type).toLong(),
                state = state.name,
                configurationVersion = configurationVersion,
                configuration = configuration,
                createdAt = now,
                updatedAt = now,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            )

        private fun stableCapabilityId(
            workspaceId: String,
            type: WorkspaceCapabilityType,
        ): String =
            LegacySubjectUuid.uuidV5(
                namespace,
                "WORKSPACE:CAPABILITY:$workspaceId:${type.name}:$DEFAULT_INSTANCE_KEY",
            ).toString()

        private fun com.romankozak.forwardappmobile.core.data.models.entities.Context.toLegacySystemWorkspaceEvidence():
            SystemWorkspaceLegacyContextEvidence =
            SystemWorkspaceLegacyContextEvidence(
                id = id,
                name = name,
                description = description,
                parentId = parentId,
                roleCode = roleCode,
                order = order,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                version = version,
            )

        private fun SystemWorkspaceLegacyContextEvidence.toCanonicalSystemWorkspace(): WorkspaceEntity =
            WorkspaceEntity(
                id = id,
                nameOverride = name,
                descriptionOverride = description,
                parentWorkspaceId = parentId,
                roleCode = roleCode,
                workspaceOrder = order,
                createdAt = createdAt,
                updatedAt = updatedAt ?: createdAt,
                syncedAt = null,
                isDeleted = false,
                version = version.coerceAtLeast(1L),
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )

        private fun SystemOperationalDefinition.toWorkspace(now: Long): WorkspaceEntity =
            WorkspaceEntity(
                id = id,
                nameOverride = defaultName,
                descriptionOverride = null,
                parentWorkspaceId = defaultParentId,
                roleCode = null,
                workspaceOrder = 0L,
                createdAt = now,
                updatedAt = now,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )
        private fun nextVersion(current: Long): Long =
            if (current == Long.MAX_VALUE) Long.MAX_VALUE else current + 1L

        private companion object {
            const val DEFAULT_INSTANCE_KEY = "default"
            val namespace: UUID = UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID)
            val capabilityOrder =
                WorkspaceCapabilityType.entries.withIndex().associate { it.value to it.index }
        }

    }
