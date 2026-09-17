package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.DirectionCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.InboxCapabilityState
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class SystemInboxDirectionState(
    val inbox: InboxCapabilityState?,
    val direction: DirectionCapabilityState?,
    val isCanonicalOwnerAvailable: Boolean = true,
) {
    val inboxEnabled: Boolean
        get() = inbox?.isActive == true
    val directionEnabled: Boolean
        get() = direction?.isActive == true
}

/**
 * Stateless authority boundary for promoted reserved System Workspace Inbox
 * and Direction settings. Canonical commands run first; the legacy
 * ContextConfiguration update is bounded compatibility output in the same Room
 * transaction and deliberately bypasses legacy-to-canonical mirrors.
 */
@Singleton
class SystemContextCanonicalInboxDirectionAccess
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextStructureDao: ContextStructureDao,
        private val inboxRepository: CanonicalInboxRepository,
        private val directionRepository: CanonicalDirectionRepository,
    ) {
        fun handles(contextId: String): Boolean = SystemContexts.isSystem(ContextId(contextId))

        suspend fun getState(contextId: String): SystemInboxDirectionState? {
            if (!handles(contextId)) return null
            val workspace = workspaceDao.getById(contextId)
            if (!isCanonicalSystemWorkspace(contextId, workspace)) {
                return unavailableState()
            }
            return SystemInboxDirectionState(
                inbox = runCatching { inboxRepository.getState(contextId) }.getOrNull(),
                direction = runCatching { directionRepository.getState(contextId) }.getOrNull(),
            )
        }

        suspend fun directionAutoLinkEnabled(contextId: String): Boolean? =
            getState(contextId)?.let { state ->
                state.directionEnabled &&
                    state.direction?.configuration?.autoLinkChildWorkspaces == true
            }

        fun observeState(contextId: String): Flow<SystemInboxDirectionState?> {
            if (!handles(contextId)) return flowOf(null)
            return combine(
                workspaceDao.observeAll(),
                inboxRepository.observeState(contextId),
                directionRepository.observeState(contextId),
            ) { workspaces, inbox, direction ->
                val workspace = workspaces.singleOrNull { it.id == contextId }
                if (isCanonicalSystemWorkspace(contextId, workspace)) {
                    SystemInboxDirectionState(inbox = inbox, direction = direction)
                } else {
                    // Reserved System identity with malformed/absent canonical ownership
                    // remains authoritative-but-unavailable and therefore fails closed.
                    unavailableState()
                }
            }
        }

        /**
         * Reconciles only the bounded legacy compatibility fields owned by an
         * established canonical System Inbox/Direction instance.
         *
         * This is called inside the ordinary ContextConfiguration write
         * transaction. Missing canonical instances leave the candidate intact
         * so legacy bootstrap may still perform the one-time seed.
         */
        suspend fun reconcileCompatibilityOutput(
            candidate: ContextConfiguration,
        ): ContextConfiguration {
            if (!handles(candidate.contextId)) return candidate
            val workspace = workspaceDao.getById(candidate.contextId) ?: return candidate
            if (workspace.provenance != WorkspaceProvenance.CANONICAL_ONLY.name) return candidate
            requireCanonicalSystemWorkspace(candidate.contextId, workspace)

            val inbox = inboxRepository.getState(candidate.contextId)
            val direction = directionRepository.getState(candidate.contextId)
            if (inbox == null && direction == null) return candidate
            val persisted = contextStructureDao.getStructureByContext(candidate.contextId)
            return candidate.copy(
                enableInbox = inbox?.isActive ?: candidate.enableInbox,
                removeInboxEntryAfterTagAutocopy =
                    inbox?.configuration?.let {
                        it.ownerVisibility == InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED
                    } ?: candidate.removeInboxEntryAfterTagAutocopy,
                experimentalCapabilityIds =
                    direction?.let {
                        candidate.experimentalCapabilityIds.withDirectionEnabled(it.isActive)
                    } ?: candidate.experimentalCapabilityIds,
                enableAutoLinkSubprojects =
                    direction?.configuration?.autoLinkChildWorkspaces
                        ?: candidate.enableAutoLinkSubprojects,
                updatedAt = maxOf(candidate.updatedAt, persisted?.updatedAt ?: candidate.updatedAt),
                version = maxOf(candidate.version, persisted?.version ?: candidate.version),
            )
        }

        suspend fun setInboxEnabled(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                if (
                    enabled ||
                    !inboxRepository.establishDisabledIfMissing(contextId, now)
                ) {
                    inboxRepository.setEnabled(contextId, enabled, now)
                }
                updateCompatibilityFromCanonical(contextId, now, includeInbox = true)
            }

        suspend fun updateInboxConfiguration(
            contextId: String,
            configuration: InboxCapabilityConfigurationV1,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                inboxRepository.updateConfiguration(contextId, configuration, now)
                updateCompatibilityFromCanonical(contextId, now, includeInbox = true)
            }

        suspend fun setDirectionEnabled(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                if (
                    enabled ||
                    !directionRepository.establishDisabledIfMissing(contextId, now)
                ) {
                    directionRepository.setEnabled(contextId, enabled, now)
                }
                updateCompatibilityFromCanonical(contextId, now, includeDirection = true)
            }

        suspend fun updateDirectionConfiguration(
            contextId: String,
            configuration: DirectionCapabilityConfigurationV1,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                directionRepository.updateConfiguration(contextId, configuration, now)
                updateCompatibilityFromCanonical(contextId, now, includeDirection = true)
            }

        private suspend fun commandIfHandled(
            contextId: String,
            command: suspend () -> Unit,
        ): Boolean {
            if (!handles(contextId)) return false
            database.withTransaction {
                requireCanonicalSystemWorkspace(contextId)
                command()
            }
            return true
        }

        private suspend fun updateCompatibilityFromCanonical(
            contextId: String,
            now: Long,
            includeInbox: Boolean = false,
            includeDirection: Boolean = false,
        ) {
            val current =
                contextStructureDao.getStructureByContext(contextId)
                    ?: ContextConfiguration.default(contextId)
            val inbox =
                if (includeInbox) {
                    requireNotNull(inboxRepository.getState(contextId)) {
                        "Canonical System Inbox state disappeared during compatibility output"
                    }
                } else {
                    null
                }
            val direction =
                if (includeDirection) {
                    requireNotNull(directionRepository.getState(contextId)) {
                        "Canonical System Direction state disappeared during compatibility output"
                    }
                } else {
                    null
                }
            val transformed =
                current.copy(
                    enableInbox = inbox?.isActive ?: current.enableInbox,
                    removeInboxEntryAfterTagAutocopy =
                        inbox?.let {
                            it.configuration.ownerVisibility == InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED
                        } ?: current.removeInboxEntryAfterTagAutocopy,
                    experimentalCapabilityIds =
                        direction?.let {
                            current.experimentalCapabilityIds.withDirectionEnabled(it.isActive)
                        } ?: current.experimentalCapabilityIds,
                    enableAutoLinkSubprojects =
                        direction?.configuration?.autoLinkChildWorkspaces
                            ?: current.enableAutoLinkSubprojects,
                )
            if (transformed == current) return
            contextStructureDao.insertStructure(
                transformed.copy(
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
            workspace: com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity?,
        ) {
            requireNotNull(workspace) { "System Workspace does not exist: $contextId" }
            require(!workspace.isDeleted) { "System Workspace is deleted: $contextId" }
            require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "System Workspace is not canonical-only: $contextId"
            }
            require(workspace.sourceContextId == null) {
                "Canonical System Workspace still references legacy Context: $contextId"
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
            SystemInboxDirectionState(
                inbox = null,
                direction = null,
                isCanonicalOwnerAvailable = false,
            )
    }

private val InboxCapabilityState.isActive: Boolean
    get() = !isDeleted && lifecycleState == WorkspaceCapabilityState.ACTIVE

private val DirectionCapabilityState.isActive: Boolean
    get() = !isDeleted && lifecycleState == WorkspaceCapabilityState.ACTIVE

private fun List<CapabilityId>.withDirectionEnabled(enabled: Boolean): List<CapabilityId> {
    val direction = CapabilityId("direction")
    return if (enabled) {
        if (direction in this) this else this + direction
    } else {
        this - direction
    }
}

/** Runtime projection over state already authorized by the focused access boundary. */
fun canonicalSystemInboxDirectionOverrides(
    contextId: String,
    state: SystemInboxDirectionState?,
): Map<CapabilityId, Boolean> {
    if (!SystemContexts.isSystem(ContextId(contextId))) return emptyMap()
    return mapOf(
        CapabilityId("inbox") to state?.inboxEnabled.orFalse(),
        CapabilityId("direction") to state?.directionEnabled.orFalse(),
    )
}

private fun Boolean?.orFalse(): Boolean = this == true
