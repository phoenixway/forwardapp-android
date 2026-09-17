package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.ConnectionsCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.InboxSortingCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.KeyProblemsCapabilityState
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class SystemRemainingCapabilityLifecycleState(
    val connections: ConnectionsCapabilityState?,
    val inboxSorting: InboxSortingCapabilityState?,
    val keyProblems: KeyProblemsCapabilityState?,
    val connectionsEstablished: Boolean,
    val inboxSortingEstablished: Boolean,
    val keyProblemsEstablished: Boolean,
    val isCanonicalOwnerAvailable: Boolean = true,
) {
    val connectionsEnabled: Boolean
        get() = connections?.isActive == true

    val inboxSortingEnabled: Boolean
        get() = inboxSorting?.isActive == true

    val keyProblemsEnabled: Boolean
        get() = keyProblems?.isActive == true
}

/**
 * Transitional canonical lifecycle boundary for the three promoted-System
 * capabilities that have no legacy-owned typed configuration semantics.
 *
 * Canonical commands run first. Only their bounded ContextConfiguration mirror
 * is written afterward, inside the same Room transaction.
 */
@Singleton
class SystemContextCanonicalRemainingCapabilityLifecycleAccess
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextStructureDao: ContextStructureDao,
        private val connectionsRepository: CanonicalConnectionsRepository,
        private val inboxSortingRepository: CanonicalInboxSortingRepository,
        private val keyProblemsRepository: CanonicalKeyProblemsRepository,
    ) {
        fun handles(contextId: String): Boolean = SystemContexts.isSystem(ContextId(contextId))

        suspend fun getState(contextId: String): SystemRemainingCapabilityLifecycleState? {
            if (!handles(contextId)) return null
            val workspace = workspaceDao.getById(contextId)
            if (!isCanonicalSystemWorkspace(contextId, workspace)) return unavailableState()

            val connectionsEstablished =
                runCatching { connectionsRepository.hasEstablishedInstance(contextId) }
                    .getOrElse { return unavailableState() }
            val inboxSortingEstablished =
                runCatching { inboxSortingRepository.hasEstablishedInstance(contextId) }
                    .getOrElse { return unavailableState() }
            val keyProblemsEstablished =
                runCatching { keyProblemsRepository.hasEstablishedInstance(contextId) }
                    .getOrElse { return unavailableState() }

            return SystemRemainingCapabilityLifecycleState(
                connections =
                    if (connectionsEstablished) {
                        runCatching { connectionsRepository.getState(contextId) }.getOrNull()
                    } else {
                        null
                    },
                inboxSorting =
                    if (inboxSortingEstablished) {
                        runCatching { inboxSortingRepository.getState(contextId) }.getOrNull()
                    } else {
                        null
                    },
                keyProblems =
                    if (keyProblemsEstablished) {
                        runCatching { keyProblemsRepository.getState(contextId) }.getOrNull()
                    } else {
                        null
                    },
                connectionsEstablished = connectionsEstablished,
                inboxSortingEstablished = inboxSortingEstablished,
                keyProblemsEstablished = keyProblemsEstablished,
            )
        }

        fun observeState(contextId: String): Flow<SystemRemainingCapabilityLifecycleState?> {
            if (!handles(contextId)) return flowOf(null)
            return combine(
                workspaceDao.observeAll(),
                observeConnections(contextId),
                observeInboxSorting(contextId),
                observeKeyProblems(contextId),
            ) { workspaces, connections, inboxSorting, keyProblems ->
                val workspace = workspaces.singleOrNull { it.id == contextId }
                if (isCanonicalSystemWorkspace(contextId, workspace)) {
                    SystemRemainingCapabilityLifecycleState(
                        connections = connections.state,
                        inboxSorting = inboxSorting.state,
                        keyProblems = keyProblems.state,
                        connectionsEstablished = connections.established,
                        inboxSortingEstablished = inboxSorting.established,
                        keyProblemsEstablished = keyProblems.established,
                    )
                } else {
                    unavailableState()
                }
            }
        }

        suspend fun reconcileCompatibilityOutput(
            candidate: ContextConfiguration,
        ): ContextConfiguration {
            if (!handles(candidate.contextId)) return candidate
            val workspace = workspaceDao.getById(candidate.contextId) ?: return candidate
            if (workspace.provenance != WorkspaceProvenance.CANONICAL_ONLY.name) return candidate
            requireCanonicalSystemWorkspace(candidate.contextId, workspace)

            val state = getState(candidate.contextId) ?: return candidate
            if (!state.isCanonicalOwnerAvailable) return candidate
            if (
                !state.connectionsEstablished &&
                !state.inboxSortingEstablished &&
                !state.keyProblemsEstablished
            ) {
                return candidate
            }

            val persisted = contextStructureDao.getStructureByContext(candidate.contextId)
            return candidate.copy(
                enableAttachments =
                    if (state.connectionsEstablished) {
                        state.connectionsEnabled
                    } else {
                        candidate.enableAttachments
                    },
                experimentalCapabilityIds =
                    candidate.experimentalCapabilityIds
                        .withCapabilityEnabled(
                            INBOX_SORTING_ID,
                            state.inboxSortingEnabled,
                            state.inboxSortingEstablished,
                        ).withCapabilityEnabled(
                            KEY_PROBLEMS_ID,
                            state.keyProblemsEnabled,
                            state.keyProblemsEstablished,
                        ),
                updatedAt = maxOf(candidate.updatedAt, persisted?.updatedAt ?: candidate.updatedAt),
                version = maxOf(candidate.version, persisted?.version ?: candidate.version),
            )
        }

        suspend fun setConnectionsEnabled(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                if (enabled || !connectionsRepository.establishDisabledIfMissing(contextId, now)) {
                    connectionsRepository.setEnabled(contextId, enabled, now)
                }
                updateCompatibilityFromCanonical(contextId, now, includeConnections = true)
            }

        suspend fun setInboxSortingEnabled(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                if (enabled || !inboxSortingRepository.establishDisabledIfMissing(contextId, now)) {
                    inboxSortingRepository.setEnabled(contextId, enabled, now)
                }
                updateCompatibilityFromCanonical(contextId, now, includeInboxSorting = true)
            }

        suspend fun setKeyProblemsEnabled(
            contextId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            commandIfHandled(contextId) {
                if (enabled || !keyProblemsRepository.establishDisabledIfMissing(contextId, now)) {
                    keyProblemsRepository.setEnabled(contextId, enabled, now)
                }
                updateCompatibilityFromCanonical(contextId, now, includeKeyProblems = true)
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
            includeConnections: Boolean = false,
            includeInboxSorting: Boolean = false,
            includeKeyProblems: Boolean = false,
        ) {
            val current =
                contextStructureDao.getStructureByContext(contextId)
                    ?: ContextConfiguration.default(contextId)
            val state = requireNotNull(getState(contextId))
            require(state.isCanonicalOwnerAvailable) {
                "Canonical System Workspace became unavailable during compatibility output"
            }
            val transformed =
                current.copy(
                    enableAttachments =
                        if (includeConnections) state.connectionsEnabled else current.enableAttachments,
                    experimentalCapabilityIds =
                        current.experimentalCapabilityIds
                            .withCapabilityEnabled(
                                INBOX_SORTING_ID,
                                state.inboxSortingEnabled,
                                includeInboxSorting,
                            ).withCapabilityEnabled(
                                KEY_PROBLEMS_ID,
                                state.keyProblemsEnabled,
                                includeKeyProblems,
                            ),
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
            require(handles(contextId)) { "Context is not a reserved System identity: $contextId" }
            require(isCanonicalSystemWorkspace(contextId, workspace)) {
                "Reserved System identity lacks a live same-id canonical Workspace: $contextId"
            }
        }

        private fun isCanonicalSystemWorkspace(
            contextId: String,
            workspace: com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity?,
        ): Boolean =
            handles(contextId) &&
                workspace?.id == contextId &&
                !workspace.isDeleted &&
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                workspace.sourceContextId == null

        private fun observeConnections(contextId: String): Flow<ObservedCapability<ConnectionsCapabilityState>> =
            combine(
                connectionsRepository.observeState(contextId),
                connectionsRepository.observeEstablishedInstance(contextId),
            ) { state, established ->
                ObservedCapability(state, established)
            }

        private fun observeInboxSorting(contextId: String): Flow<ObservedCapability<InboxSortingCapabilityState>> =
            combine(
                inboxSortingRepository.observeState(contextId),
                inboxSortingRepository.observeEstablishedInstance(contextId),
            ) { state, established ->
                ObservedCapability(state, established)
            }

        private fun observeKeyProblems(contextId: String): Flow<ObservedCapability<KeyProblemsCapabilityState>> =
            combine(
                keyProblemsRepository.observeState(contextId),
                keyProblemsRepository.observeEstablishedInstance(contextId),
            ) { state, established ->
                ObservedCapability(state, established)
            }

        private fun unavailableState() =
            SystemRemainingCapabilityLifecycleState(
                connections = null,
                inboxSorting = null,
                keyProblems = null,
                connectionsEstablished = true,
                inboxSortingEstablished = true,
                keyProblemsEstablished = true,
                isCanonicalOwnerAvailable = false,
            )

        private data class ObservedCapability<T>(
            val state: T?,
            val established: Boolean,
        )

        private companion object {
            val INBOX_SORTING_ID = CapabilityId("inbox_sorting")
            val KEY_PROBLEMS_ID = CapabilityId("key_problems")
        }
    }

fun canonicalSystemRemainingCapabilityOverrides(
    contextId: String,
    state: SystemRemainingCapabilityLifecycleState?,
): Map<CapabilityId, Boolean> {
    if (!SystemContexts.isSystem(ContextId(contextId))) return emptyMap()
    if (state == null || !state.isCanonicalOwnerAvailable) {
        return mapOf(
            CONNECTIONS_ID to false,
            INBOX_SORTING_ID to false,
            KEY_PROBLEMS_ID to false,
        )
    }
    return buildMap {
        if (state.connectionsEstablished) put(CONNECTIONS_ID, state.connectionsEnabled)
        if (state.inboxSortingEstablished) put(INBOX_SORTING_ID, state.inboxSortingEnabled)
        if (state.keyProblemsEstablished) put(KEY_PROBLEMS_ID, state.keyProblemsEnabled)
    }
}

private val ConnectionsCapabilityState.isActive: Boolean
    get() = !isDeleted && lifecycleState == WorkspaceCapabilityState.ACTIVE

private val InboxSortingCapabilityState.isActive: Boolean
    get() = !isDeleted && lifecycleState == WorkspaceCapabilityState.ACTIVE

private val KeyProblemsCapabilityState.isActive: Boolean
    get() = !isDeleted && lifecycleState == WorkspaceCapabilityState.ACTIVE

private fun List<CapabilityId>.withCapabilityEnabled(
    capabilityId: CapabilityId,
    enabled: Boolean,
    apply: Boolean,
): List<CapabilityId> {
    if (!apply) return this
    return if (enabled) {
        (this + capabilityId).distinct()
    } else {
        this - capabilityId
    }
}

private val CONNECTIONS_ID = CapabilityId("connections")
private val INBOX_SORTING_ID = CapabilityId("inbox_sorting")
private val KEY_PROBLEMS_ID = CapabilityId("key_problems")
