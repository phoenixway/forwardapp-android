package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class SystemBacklogConfigurationState(
    val configuration: BacklogCapabilityConfigurationV2?,
    val isEstablished: Boolean,
    val isCanonicalOwnerAvailable: Boolean = true,
) {
    val removeEntryAfterTagAutocopy: Boolean
        get() = configuration?.removeEntryAfterTagAutocopy == true
}

/**
 * Canonical BACKLOG behavior boundary for promoted reserved System Workspaces.
 *
 * V1 is seedable historical state. V2 is canonical authority. User commands
 * write canonical state first and then the bounded legacy compatibility field
 * in the same Room transaction.
 */
@Singleton
class SystemContextCanonicalBacklogConfigurationAccess
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextStructureDao: ContextStructureDao,
        private val backlogRepository: CanonicalBacklogRepository,
    ) {
        fun handles(contextId: String): Boolean = SystemContexts.isSystem(ContextId(contextId))

        suspend fun getState(contextId: String): SystemBacklogConfigurationState? {
            if (!handles(contextId)) return null
            if (!isCanonicalSystemWorkspace(contextId, workspaceDao.getById(contextId))) {
                return unavailableState()
            }
            val established =
                runCatching { backlogRepository.hasEstablishedInstance(contextId) }
                    .getOrElse { return unavailableState() }
            if (!established) return SystemBacklogConfigurationState(null, isEstablished = false)
            val state = runCatching { backlogRepository.getState(contextId) }.getOrNull()
            return SystemBacklogConfigurationState(
                configuration = state?.configuration as? BacklogCapabilityConfigurationV2,
                isEstablished = true,
            )
        }

        fun observeState(contextId: String): Flow<SystemBacklogConfigurationState?> {
            if (!handles(contextId)) return flowOf(null)
            return combine(
                workspaceDao.observeAll(),
                backlogRepository.observeEstablishedInstance(contextId),
                backlogRepository.observeState(contextId),
            ) { workspaces, established, state ->
                if (!isCanonicalSystemWorkspace(contextId, workspaces.singleOrNull { it.id == contextId })) {
                    unavailableState()
                } else {
                    SystemBacklogConfigurationState(
                        configuration = state?.configuration as? BacklogCapabilityConfigurationV2,
                        isEstablished = established,
                    )
                }
            }
        }

        suspend fun setRemoveEntryAfterTagAutocopy(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            if (!handles(contextId)) return false
            database.withTransaction {
                requireCanonicalSystemWorkspace(contextId)
                val current =
                    requireNotNull(backlogRepository.getState(contextId)) {
                        "Canonical System BACKLOG capability does not exist: $contextId"
                    }
                if (current.configuration is BacklogCapabilityConfigurationV1) {
                    val legacySeed =
                        contextStructureDao.getStructureByContext(contextId)
                            ?.removeBacklogEntryAfterTagAutocopy == true
                    backlogRepository.migrateV1Configuration(
                        contextId,
                        BacklogCapabilityConfigurationV2(legacySeed),
                        now,
                    )
                }
                backlogRepository.updateConfiguration(
                    contextId,
                    BacklogCapabilityConfigurationV2(enabled),
                    now,
                )
                updateCompatibilityOutput(contextId, enabled, now)
            }
            return true
        }

        suspend fun reconcileCompatibilityOutput(candidate: ContextConfiguration): ContextConfiguration {
            val state = getState(candidate.contextId) ?: return candidate
            if (!state.isCanonicalOwnerAvailable || !state.isEstablished) return candidate
            val canonical = state.configuration ?: return candidate
            val persisted = contextStructureDao.getStructureByContext(candidate.contextId)
            return candidate.copy(
                removeBacklogEntryAfterTagAutocopy = canonical.removeEntryAfterTagAutocopy,
                updatedAt = maxOf(candidate.updatedAt, persisted?.updatedAt ?: candidate.updatedAt),
                version = maxOf(candidate.version, persisted?.version ?: candidate.version),
            )
        }

        private suspend fun updateCompatibilityOutput(
            contextId: String,
            enabled: Boolean,
            now: Long,
        ) {
            val current =
                contextStructureDao.getStructureByContext(contextId)
                    ?: ContextConfiguration.default(contextId)
            if (current.removeBacklogEntryAfterTagAutocopy == enabled) return
            contextStructureDao.insertStructure(
                current.copy(
                    removeBacklogEntryAfterTagAutocopy = enabled,
                    updatedAt = now,
                    version = current.version + 1L,
                ),
            )
        }

        private suspend fun requireCanonicalSystemWorkspace(contextId: String) {
            val workspace = requireNotNull(workspaceDao.getById(contextId)) {
                "System Workspace does not exist: $contextId"
            }
            require(isCanonicalSystemWorkspace(contextId, workspace)) {
                "System Workspace is not a live promoted canonical owner: $contextId"
            }
        }

        private fun isCanonicalSystemWorkspace(
            contextId: String,
            workspace: com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity?,
        ): Boolean =
            SystemContexts.isSystem(ContextId(contextId)) &&
                workspace?.id == contextId &&
                !workspace.isDeleted &&
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                workspace.sourceContextId == null

        private fun unavailableState() =
            SystemBacklogConfigurationState(
                configuration = null,
                isEstablished = true,
                isCanonicalOwnerAvailable = false,
            )
}
