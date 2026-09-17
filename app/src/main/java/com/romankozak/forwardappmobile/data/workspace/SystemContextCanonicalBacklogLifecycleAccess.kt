package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class SystemBacklogLifecycleState(
    val backlog: BacklogCapabilityState?,
    val isEstablished: Boolean,
    val isCanonicalOwnerAvailable: Boolean = true,
) {
    val enabled: Boolean
        get() = backlog?.isActive == true
}

/**
 * Transitional lifecycle boundary for promoted-System BACKLOG.
 *
 * Canonical lifecycle commands run first. The bounded legacy enableBacklog
 * field is compatibility output in the same Room transaction.
 */
@Singleton
class SystemContextCanonicalBacklogLifecycleAccess
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextStructureDao: ContextStructureDao,
        private val backlogRepository: CanonicalBacklogRepository,
    ) {
        fun handles(contextId: String): Boolean = SystemContexts.isSystem(ContextId(contextId))

        suspend fun getState(contextId: String): SystemBacklogLifecycleState? {
            if (!handles(contextId)) return null
            if (!isCanonicalSystemWorkspace(contextId, workspaceDao.getById(contextId))) {
                return unavailableState()
            }
            val established =
                runCatching { backlogRepository.hasEstablishedInstance(contextId) }
                    .getOrElse { return unavailableState() }
            return SystemBacklogLifecycleState(
                backlog =
                    if (established) {
                        runCatching { backlogRepository.getState(contextId) }.getOrNull()
                    } else {
                        null
                    },
                isEstablished = established,
            )
        }

        fun observeState(contextId: String): Flow<SystemBacklogLifecycleState?> {
            if (!handles(contextId)) return flowOf(null)
            return combine(
                workspaceDao.observeAll(),
                backlogRepository.observeEstablishedInstance(contextId),
                backlogRepository.observeState(contextId),
            ) { workspaces, established, state ->
                if (isCanonicalSystemWorkspace(contextId, workspaces.singleOrNull { it.id == contextId })) {
                    SystemBacklogLifecycleState(
                        backlog = if (established) state else null,
                        isEstablished = established,
                    )
                } else {
                    unavailableState()
                }
            }
        }

        suspend fun setEnabled(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            if (!handles(contextId)) return false
            database.withTransaction {
                requireCanonicalSystemWorkspace(contextId)
                if (enabled || !backlogRepository.establishDisabledIfMissing(contextId, now)) {
                    backlogRepository.setEnabled(contextId, enabled, now)
                }
                updateCompatibilityFromCanonical(contextId, now)
            }
            return true
        }

        suspend fun reconcileCompatibilityOutput(
            candidate: ContextConfiguration,
        ): ContextConfiguration {
            if (!handles(candidate.contextId)) return candidate
            val workspace = workspaceDao.getById(candidate.contextId) ?: return candidate
            if (workspace.provenance != WorkspaceProvenance.CANONICAL_ONLY.name) return candidate
            requireCanonicalSystemWorkspace(candidate.contextId, workspace)

            val state = requireNotNull(getState(candidate.contextId))
            if (!state.isCanonicalOwnerAvailable || !state.isEstablished) return candidate
            val persisted = contextStructureDao.getStructureByContext(candidate.contextId)
            return candidate.copy(
                enableBacklog = state.enabled,
                updatedAt = maxOf(candidate.updatedAt, persisted?.updatedAt ?: candidate.updatedAt),
                version = maxOf(candidate.version, persisted?.version ?: candidate.version),
            )
        }

        private suspend fun updateCompatibilityFromCanonical(
            contextId: String,
            now: Long,
        ) {
            val state = requireNotNull(getState(contextId))
            require(state.isCanonicalOwnerAvailable && state.isEstablished) {
                "Canonical System BACKLOG state became unavailable during compatibility output"
            }
            val current =
                contextStructureDao.getStructureByContext(contextId)
                    ?: ContextConfiguration.default(contextId)
            if (current.enableBacklog == state.enabled) return
            contextStructureDao.insertStructure(
                current.copy(
                    enableBacklog = state.enabled,
                    updatedAt = now,
                    version = current.version + 1L,
                ),
            )
        }

        private suspend fun requireCanonicalSystemWorkspace(contextId: String) {
            requireCanonicalSystemWorkspace(contextId, workspaceDao.getById(contextId))
        }

        private fun requireCanonicalSystemWorkspace(
            contextId: String,
            workspace: WorkspaceEntity?,
        ) {
            require(handles(contextId)) { "Context is not a reserved System identity: $contextId" }
            require(isCanonicalSystemWorkspace(contextId, workspace)) {
                "Reserved System identity lacks a live same-id canonical Workspace: $contextId"
            }
        }

        private fun isCanonicalSystemWorkspace(
            contextId: String,
            workspace: WorkspaceEntity?,
        ): Boolean =
            handles(contextId) &&
                workspace?.id == contextId &&
                !workspace.isDeleted &&
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                workspace.sourceContextId == null

        private fun unavailableState() =
            SystemBacklogLifecycleState(
                backlog = null,
                isEstablished = true,
                isCanonicalOwnerAvailable = false,
            )
    }

fun canonicalSystemBacklogLifecycleOverrides(
    contextId: String,
    state: SystemBacklogLifecycleState?,
): Map<CapabilityId, Boolean> {
    if (!SystemContexts.isSystem(ContextId(contextId))) return emptyMap()
    if (state == null || !state.isCanonicalOwnerAvailable) {
        return mapOf(BACKLOG_ID to false)
    }
    return if (state.isEstablished) mapOf(BACKLOG_ID to state.enabled) else emptyMap()
}

private val BacklogCapabilityState.isActive: Boolean
    get() = !isDeleted && lifecycleState == WorkspaceCapabilityState.ACTIVE

private val BACKLOG_ID = CapabilityId("backlog")
