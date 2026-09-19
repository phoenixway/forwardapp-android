package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import javax.inject.Inject
import javax.inject.Singleton

/** Settings-screen projection and command coordinator over canonical typed capability owners. */
@Singleton
class CanonicalOrdinaryCapabilitySettings
    @Inject
    constructor(
        private val workspaceRepository: CanonicalWorkspaceRepository,
        private val backlogRepository: CanonicalBacklogRepository,
        private val inboxRepository: CanonicalInboxRepository,
        private val directionRepository: CanonicalDirectionRepository,
        private val connectionsRepository: CanonicalConnectionsRepository,
        private val inboxSortingRepository: CanonicalInboxSortingRepository,
        private val keyProblemsRepository: CanonicalKeyProblemsRepository,
        private val dashboardRepository: CanonicalDashboardCapabilityRepository,
        private val executionLogRepository: CanonicalExecutionLogRepository,
    ) {
        suspend fun loadEnabledOrNull(workspaceId: String): Set<CapabilityId>? {
            if (!owns(workspaceId)) return null
            val backlog = backlogRepository.getState(workspaceId)
            val inbox = inboxRepository.getState(workspaceId)
            val direction = directionRepository.getState(workspaceId)
            val connections = connectionsRepository.getState(workspaceId)
            val inboxSorting = inboxSortingRepository.getState(workspaceId)
            val keyProblems = keyProblemsRepository.getState(workspaceId)
            val dashboardEnabled = dashboardRepository.isEnabled(workspaceId)
            val executionLogEnabled = executionLogRepository.isEnabled(workspaceId)
            return buildSet {
                backlog
                    ?.takeIf { !it.isDeleted && it.lifecycleState == WorkspaceCapabilityState.ACTIVE }
                    ?.let { add(CapabilityId(BACKLOG)) }
                inbox
                    ?.takeIf { !it.isDeleted && it.lifecycleState == WorkspaceCapabilityState.ACTIVE }
                    ?.let { add(CapabilityId(INBOX)) }
                direction
                    ?.takeIf { !it.isDeleted && it.lifecycleState == WorkspaceCapabilityState.ACTIVE }
                    ?.let { add(CapabilityId(DIRECTION)) }
                connections
                    ?.takeIf { !it.isDeleted && it.lifecycleState == WorkspaceCapabilityState.ACTIVE }
                    ?.let { add(CapabilityId(CONNECTIONS)) }
                inboxSorting
                    ?.takeIf { !it.isDeleted && it.lifecycleState == WorkspaceCapabilityState.ACTIVE }
                    ?.let { add(CapabilityId(INBOX_SORTING)) }
                keyProblems
                    ?.takeIf { !it.isDeleted && it.lifecycleState == WorkspaceCapabilityState.ACTIVE }
                    ?.let { add(CapabilityId(KEY_PROBLEMS)) }
                if (dashboardEnabled) add(CapabilityId(DASHBOARD))
                if (executionLogEnabled) add(CapabilityId(EXECUTION_LOG))
            }
        }

        suspend fun persistIfOwned(
            workspaceId: String,
            enabledCapabilityIds: Set<CapabilityId>,
        ): Boolean {
            if (!owns(workspaceId)) return false
            val enabled = enabledCapabilityIds.mapTo(mutableSetOf()) { it.raw }
            backlogRepository.setEnabled(workspaceId, BACKLOG in enabled)
            inboxRepository.setEnabled(workspaceId, INBOX in enabled)
            directionRepository.setEnabled(workspaceId, DIRECTION in enabled)
            connectionsRepository.setEnabled(workspaceId, CONNECTIONS in enabled)
            inboxSortingRepository.setEnabled(workspaceId, INBOX_SORTING in enabled)
            keyProblemsRepository.setEnabled(workspaceId, KEY_PROBLEMS in enabled)
            dashboardRepository.setEnabled(workspaceId, DASHBOARD in enabled)
            executionLogRepository.setEnabled(workspaceId, EXECUTION_LOG in enabled)
            return true
        }

        private suspend fun owns(workspaceId: String): Boolean =
            !SystemContexts.isSystem(ContextId(workspaceId)) &&
                workspaceRepository.getCanonicalPresentation(workspaceId)?.isDeleted == false

        private companion object {
            const val BACKLOG = "backlog"
            const val INBOX = "inbox"
            const val DIRECTION = "direction"
            const val CONNECTIONS = "connections"
            const val INBOX_SORTING = "inbox_sorting"
            const val KEY_PROBLEMS = "key_problems"
            const val DASHBOARD = "dashboard"
            const val EXECUTION_LOG = "log"
        }
    }
