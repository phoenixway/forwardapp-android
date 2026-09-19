package com.romankozak.forwardappmobile.features.contexts.shared

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.SystemRemainingCapabilityLifecycleState
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityReadSnapshot
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import java.util.UUID
import kotlinx.coroutines.flow.first

class AndroidWorkspaceRepositoryAdapter(
    private val contextRepository: ContextRepository,
    private val goalRepository: GoalRepository,
    private val contextStructureRepository: ContextStructureRepository,
    private val canonicalWorkspaceBootstrapper: CanonicalWorkspaceBootstrapper,
    private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
    private val canonicalCapabilityInstanceStore: CanonicalCapabilityInstanceStore,
    private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
    private val canonicalBacklogRepository: CanonicalBacklogRepository,
    private val canonicalInboxRepository: CanonicalInboxRepository,
    private val canonicalDirectionRepository: CanonicalDirectionRepository,
    private val canonicalConnectionsRepository: CanonicalConnectionsRepository,
    private val canonicalInboxSortingRepository: CanonicalInboxSortingRepository,
    private val canonicalKeyProblemsRepository: CanonicalKeyProblemsRepository,
    private val canonicalDashboardCapabilityRepository: CanonicalDashboardCapabilityRepository,
    private val canonicalExecutionLogRepository: CanonicalExecutionLogRepository,
    private val systemInboxDirectionAccess: SystemContextCanonicalInboxDirectionAccess,
    private val systemRemainingCapabilityAccess: SystemContextCanonicalRemainingCapabilityLifecycleAccess,
    private val systemBacklogLifecycleAccess: SystemContextCanonicalBacklogLifecycleAccess,
) : DesktopWorkspaceRepository {
    override suspend fun getContexts(): List<SharedContextSummary> {
        canonicalWorkspaceBootstrapper.ensureBootstrapped()

        val rawContexts =
            contextRepository.getAllContextsFlow()
                .first()
                .filterNot { context -> context.isDeleted }
        val canonicalPresentations = canonicalWorkspaceRepository.getCanonicalPresentations()
        val capabilitySnapshot = canonicalCapabilityInstanceStore.loadReadSnapshot()

        return systemWorkspacePresentationContextProjector
            .projectPresentationUniverse(rawContexts)
            .sortedBy { presentation -> presentation.order }
            .mapNotNull { presentation ->
                if (SystemContexts.isSystem(ContextId(presentation.id))) {
                    toSystemSharedSummary(
                        presentation = presentation,
                        configuration = contextStructureRepository.getStructureByContext(presentation.id),
                    )
                } else {
                    canonicalPresentations[presentation.id]
                        ?.takeIf { canonical -> !canonical.isDeleted }
                        ?.let {
                            toCanonicalSharedSummary(
                                presentation = presentation,
                                capabilitySnapshot = capabilitySnapshot,
                            )
                        }
                }
            }
    }

    override suspend fun createContext(
        parentId: String?,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String>,
        experimentalCapabilityIds: List<String>,
    ): SharedContextSummary {
        canonicalWorkspaceBootstrapper.ensureBootstrapped()

        val workspaceId =
            canonicalWorkspaceRepository.create(
                nameOverride = name,
                descriptionOverride = description,
                parentWorkspaceId = parentId,
            )

        val requestedCapabilities =
            requestedCapabilityIds(
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )
        applyCanonicalCapabilitySelection(workspaceId, requestedCapabilities)

        val presentation =
            systemWorkspacePresentationContextProjector.resolvePresentation(
                contextId = workspaceId,
                context = null,
            ) ?: error("Canonical Workspace presentation unavailable after creation: $workspaceId")

        return toCanonicalSharedSummary(presentation)
    }

    override suspend fun updateContext(
        contextId: String,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String>,
        experimentalCapabilityIds: List<String>,
    ): SharedContextSummary? {
        canonicalWorkspaceBootstrapper.ensureBootstrapped()

        val requestedCapabilities =
            requestedCapabilityIds(
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )

        if (SystemContexts.isSystem(ContextId(contextId))) {
            if (systemInboxDirectionAccess.handles(contextId)) {
                systemInboxDirectionAccess.setInboxEnabled(contextId, "inbox" in requestedCapabilities)
                systemInboxDirectionAccess.setDirectionEnabled(contextId, "direction" in requestedCapabilities)
            }
            if (systemRemainingCapabilityAccess.handles(contextId)) {
                systemRemainingCapabilityAccess.setConnectionsEnabled(contextId, "connections" in requestedCapabilities)
                systemRemainingCapabilityAccess.setInboxSortingEnabled(contextId, "inbox_sorting" in requestedCapabilities)
                systemRemainingCapabilityAccess.setKeyProblemsEnabled(contextId, "key_problems" in requestedCapabilities)
            }
            if (systemBacklogLifecycleAccess.handles(contextId)) {
                systemBacklogLifecycleAccess.setEnabled(contextId, "backlog" in requestedCapabilities)
            }

            contextRepository.updateContextPresentation(contextId, name, description)
            canonicalDashboardCapabilityRepository.setEnabled(contextId, "dashboard" in requestedCapabilities)
            canonicalExecutionLogRepository.setEnabled(contextId, "log" in requestedCapabilities)

            val presented =
                systemWorkspacePresentationContextProjector.resolvePresentation(
                    contextId = contextId,
                    context = null,
                ) ?: return null

            return toSystemSharedSummary(
                presentation = presented,
                configuration = contextStructureRepository.getStructureByContext(contextId),
            )
        }

        val canonical = canonicalWorkspaceRepository.getCanonicalPresentation(contextId)
        require(canonical != null && !canonical.isDeleted) {
            "Ordinary CONTEXT_BACKED Workspace authoring is retired: $contextId"
        }

        canonicalWorkspaceRepository.updateNameAndDescription(
            id = contextId,
            nameOverride = name,
            descriptionOverride = description,
        )
        applyCanonicalCapabilitySelection(contextId, requestedCapabilities)

        val presented =
            systemWorkspacePresentationContextProjector.resolvePresentation(
                contextId = contextId,
                context = null,
            ) ?: return null

        return toCanonicalSharedSummary(presented)
    }

    override suspend fun deleteContext(contextId: String): Boolean {
        if (SystemContexts.isSystem(ContextId(contextId))) return false

        canonicalWorkspaceBootstrapper.ensureBootstrapped()
        val canonical = canonicalWorkspaceRepository.getCanonicalPresentation(contextId) ?: return false
        if (canonical.isDeleted) return false

        canonicalWorkspaceRepository.tombstone(contextId)
        return true
    }

    override suspend fun getBacklogItems(contextId: String): List<SharedBacklogItem> =
        goalRepository.getGoalsByContextIdFlow(contextId)
            .first()
            .filterNot { goal -> goal.isDeleted }
            .sortedByDescending { goal -> goal.updatedAt ?: goal.createdAt }
            .map { goal -> goal.toSharedBacklogItem(contextId) }

    override suspend fun createBacklogItem(
        contextId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem {
        val now = System.currentTimeMillis()
        val goal =
            Goal(
                id = UUID.randomUUID().toString(),
                text = title,
                description = details,
                completed = false,
                goalStatus = priority.toGoalStatus(),
                createdAt = now,
                updatedAt = now,
            )
        goalRepository.addGoalToContext(goal, contextId)
        return requireNotNull(goalRepository.getGoalById(goal.id)).toSharedBacklogItem(contextId)
    }

    override suspend fun updateBacklogItemDone(
        itemId: String,
        isDone: Boolean,
    ): SharedBacklogItem? {
        val goal = goalRepository.getGoalById(itemId) ?: return null
        val updated =
            goal.copy(
                completed = isDone,
                goalStatus = if (isDone) GoalStatusValues.DONE else GoalStatusValues.ACTIVE,
                updatedAt = System.currentTimeMillis(),
            )
        goalRepository.updateGoal(updated)
        val contextId = goalRepository.findContextIdForGoal(itemId).orEmpty()
        return goalRepository.getGoalById(itemId)?.toSharedBacklogItem(contextId)
    }

    override suspend fun updateBacklogItemContent(
        itemId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem? {
        val goal = goalRepository.getGoalById(itemId) ?: return null
        val updated =
            goal.copy(
                text = title,
                description = details,
                goalStatus = priority.toGoalStatus(goal.completed),
                updatedAt = System.currentTimeMillis(),
            )
        goalRepository.updateGoal(updated)
        val contextId = goalRepository.findContextIdForGoal(itemId).orEmpty()
        return goalRepository.getGoalById(itemId)?.toSharedBacklogItem(contextId)
    }

    override suspend fun deleteBacklogItem(itemId: String): Boolean {
        val goal = goalRepository.getGoalById(itemId) ?: return false
        goalRepository.deleteGoal(goal.id)
        return true
    }

    private suspend fun applyCanonicalCapabilitySelection(
        workspaceId: String,
        requestedCapabilities: List<String>,
    ) {
        val requested = requestedCapabilities.toSet()

        canonicalBacklogRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "backlog" in requested,
        )
        canonicalInboxRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "inbox" in requested,
        )
        canonicalDirectionRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "direction" in requested,
        )
        canonicalConnectionsRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "connections" in requested,
        )
        canonicalInboxSortingRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "inbox_sorting" in requested,
        )
        canonicalKeyProblemsRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "key_problems" in requested,
        )
        canonicalDashboardCapabilityRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "dashboard" in requested,
        )
        canonicalExecutionLogRepository.setEnabled(
            workspaceId = workspaceId,
            enabled = "log" in requested,
        )
    }

    private suspend fun toCanonicalSharedSummary(
        presentation: ContextPresentation,
    ): SharedContextSummary =
        toCanonicalSharedSummary(
            presentation = presentation,
            capabilitySnapshot = canonicalCapabilityInstanceStore.loadReadSnapshot(),
        )

    private fun toCanonicalSharedSummary(
        presentation: ContextPresentation,
        capabilitySnapshot: CanonicalCapabilityReadSnapshot,
    ): SharedContextSummary {
        val activeCapabilityIds =
            canonicalActiveCapabilityIds(
                workspaceId = presentation.id,
                capabilitySnapshot = capabilitySnapshot,
            )

        return SharedContextSummary(
            id = presentation.id,
            name = presentation.name,
            description = presentation.description,
            parentId = presentation.parentId,
            // Transitional shared-contract placeholders only.
            // These are not persisted as Workspace properties.
            status = SharedContextStatus.NoPlan,
            defaultView = SharedContextView.Backlog,
            score = 0,
            isCompleted = false,
            enabledCapabilityIds =
                activeCapabilityIds.filter { capabilityId ->
                    capabilityId in LEGACY_ANDROID_CAPABILITY_IDS
                },
            experimentalCapabilityIds =
                activeCapabilityIds.filterNot { capabilityId ->
                    capabilityId in LEGACY_ANDROID_CAPABILITY_IDS
                },
        )
    }

    private fun canonicalActiveCapabilityIds(
        workspaceId: String,
        capabilitySnapshot: CanonicalCapabilityReadSnapshot,
    ): List<String> =
        SharedContextCapabilityCatalog.normalizeCapabilityIds(
            buildList {
                canonicalBacklogRepository.getState(workspaceId, capabilitySnapshot)?.let { state ->
                    if (!state.isDeleted && state.lifecycleState == WorkspaceCapabilityState.ACTIVE) {
                        add("backlog")
                    }
                }
                canonicalInboxRepository.getState(workspaceId, capabilitySnapshot)?.let { state ->
                    if (!state.isDeleted && state.lifecycleState == WorkspaceCapabilityState.ACTIVE) {
                        add("inbox")
                    }
                }
                canonicalDirectionRepository.getState(workspaceId, capabilitySnapshot)?.let { state ->
                    if (!state.isDeleted && state.lifecycleState == WorkspaceCapabilityState.ACTIVE) {
                        add("direction")
                    }
                }
                canonicalConnectionsRepository.getState(workspaceId, capabilitySnapshot)?.let { state ->
                    if (!state.isDeleted && state.lifecycleState == WorkspaceCapabilityState.ACTIVE) {
                        add("connections")
                    }
                }
                canonicalInboxSortingRepository.getState(workspaceId, capabilitySnapshot)?.let { state ->
                    if (!state.isDeleted && state.lifecycleState == WorkspaceCapabilityState.ACTIVE) {
                        add("inbox_sorting")
                    }
                }
                canonicalKeyProblemsRepository.getState(workspaceId, capabilitySnapshot)?.let { state ->
                    if (!state.isDeleted && state.lifecycleState == WorkspaceCapabilityState.ACTIVE) {
                        add("key_problems")
                    }
                }
                if (canonicalDashboardCapabilityRepository.isEnabled(workspaceId, capabilitySnapshot)) {
                    add("dashboard")
                }
                if (canonicalExecutionLogRepository.isEnabled(workspaceId, capabilitySnapshot)) {
                    add("log")
                }
            },
        )

    private suspend fun toSystemSharedSummary(
        presentation: ContextPresentation,
        configuration: ContextConfiguration?,
    ): SharedContextSummary {
        val systemState = systemInboxDirectionAccess.getState(presentation.id)
        val remainingSystemState = systemRemainingCapabilityAccess.getState(presentation.id)
        val systemBacklogState = systemBacklogLifecycleAccess.getState(presentation.id)
        val defaultView = SharedContextView.Backlog

        return SharedContextSummary(
            id = presentation.id,
            name = presentation.name,
            description = presentation.description,
            parentId = presentation.parentId,
            status = SharedContextStatus.NoPlan,
            defaultView = defaultView,
            score = 0,
            isCompleted = false,
            enabledCapabilityIds =
                configuration.enabledCapabilityIds(
                    defaultView = defaultView,
                    dashboardEnabled = canonicalDashboardCapabilityRepository.isEnabled(presentation.id),
                    executionLogEnabled = canonicalExecutionLogRepository.isEnabled(presentation.id),
                    systemInboxDirectionState = systemState,
                    systemRemainingCapabilityState = remainingSystemState,
                    systemBacklogLifecycleState = systemBacklogState,
                ),
            experimentalCapabilityIds =
                configuration.experimentalCapabilityIds(
                    systemState,
                    remainingSystemState,
                    systemBacklogState,
                ),
        )
    }
}

private fun ContextConfiguration?.enabledCapabilityIds(
    defaultView: SharedContextView,
    dashboardEnabled: Boolean,
    executionLogEnabled: Boolean,
    systemInboxDirectionState: SystemInboxDirectionState?,
    systemRemainingCapabilityState: SystemRemainingCapabilityLifecycleState?,
    systemBacklogLifecycleState: SystemBacklogLifecycleState?,
): List<String> {
    val legacyExplicitIds =
        buildList {
            if (this@enabledCapabilityIds?.enableInbox == true) add("inbox")
            if (this@enabledCapabilityIds?.enableLog == true) add("log")
            if (this@enabledCapabilityIds?.enableDashboard == true) add("dashboard")
            if (this@enabledCapabilityIds?.enableBacklog == true) add("backlog")
            if (this@enabledCapabilityIds?.enableAttachments == true) add("connections")
        }
    val fallbackIds =
        if (legacyExplicitIds.isEmpty()) {
            SharedContextCapabilityCatalog.defaultCapabilityIdsFor(defaultView)
        } else {
            emptyList()
        }
    val withoutCanonicalDashboardOrExecutionLog =
        SharedContextCapabilityCatalog.normalizeCapabilityIds(
            legacyExplicitIds.filterNot { capabilityId ->
                capabilityId == "dashboard" || capabilityId == "log"
            } +
                fallbackIds +
                SharedContextCapabilityCatalog.capabilityIdFor(defaultView),
        ).filterNot { capabilityId ->
            capabilityId == "dashboard" || capabilityId == "log"
        }

    val canonicalCapabilityIds =
        buildList {
            addAll(withoutCanonicalDashboardOrExecutionLog)
            if (dashboardEnabled) add("dashboard")
            if (executionLogEnabled) add("log")
        }

    val normalized = SharedContextCapabilityCatalog.normalizeCapabilityIds(canonicalCapabilityIds)
    val withInboxAndDirection =
        if (systemInboxDirectionState == null) {
            normalized
        } else {
            val directionWasRepresentedAsEnabled = "direction" in normalized
            SharedContextCapabilityCatalog.normalizeCapabilityIds(
                buildList {
                    addAll(normalized.filterNot { it == "inbox" || it == "direction" })
                    if (systemInboxDirectionState.inboxEnabled) add("inbox")
                    if (systemInboxDirectionState.directionEnabled && directionWasRepresentedAsEnabled) {
                        add("direction")
                    }
                },
            )
        }
    val withRemaining =
        if (systemRemainingCapabilityState == null) {
            withInboxAndDirection
        } else {
            val keyProblemsWasRepresentedAsEnabled = "key_problems" in withInboxAndDirection
            SharedContextCapabilityCatalog.normalizeCapabilityIds(
                buildList {
                    addAll(
                        withInboxAndDirection.filterNot { capabilityId ->
                            (capabilityId == "connections" && systemRemainingCapabilityState.connectionsEstablished) ||
                                (capabilityId == "inbox_sorting" && systemRemainingCapabilityState.inboxSortingEstablished) ||
                                (capabilityId == "key_problems" && systemRemainingCapabilityState.keyProblemsEstablished)
                        },
                    )
                    if (
                        systemRemainingCapabilityState.connectionsEstablished &&
                        systemRemainingCapabilityState.connectionsEnabled
                    ) {
                        add("connections")
                    }
                    if (
                        systemRemainingCapabilityState.keyProblemsEstablished &&
                        systemRemainingCapabilityState.keyProblemsEnabled &&
                        keyProblemsWasRepresentedAsEnabled
                    ) {
                        add("key_problems")
                    }
                },
            )
        }
    if (systemBacklogLifecycleState == null || !systemBacklogLifecycleState.isEstablished) {
        return withRemaining
    }
    return SharedContextCapabilityCatalog.normalizeCapabilityIds(
        buildList {
            addAll(withRemaining.filterNot { it == "backlog" })
            if (systemBacklogLifecycleState.enabled) add("backlog")
        },
    )
}

private fun requestedCapabilityIds(
    enabledCapabilityIds: List<String>,
    experimentalCapabilityIds: List<String>,
): List<String> =
    SharedContextCapabilityCatalog.normalizeCapabilityIds(
        enabledCapabilityIds + experimentalCapabilityIds,
    )

private fun ContextConfiguration?.experimentalCapabilityIds(
    systemInboxDirectionState: SystemInboxDirectionState?,
    systemRemainingCapabilityState: SystemRemainingCapabilityLifecycleState?,
    systemBacklogLifecycleState: SystemBacklogLifecycleState?,
): List<String> {
    val normalized =
        SharedContextCapabilityCatalog.normalizeCapabilityIds(
        this
            ?.experimentalCapabilityIds
            .orEmpty()
            .map { capabilityId -> capabilityId.raw },
        )
    val withDirection =
        if (systemInboxDirectionState == null) {
            normalized
        } else {
            SharedContextCapabilityCatalog.normalizeCapabilityIds(
                buildList {
                    addAll(normalized.filterNot { it == "inbox" || it == "direction" })
                    if (systemInboxDirectionState.directionEnabled) add("direction")
                },
            )
        }
    val withRemaining =
        if (systemRemainingCapabilityState == null) {
            withDirection
        } else {
            SharedContextCapabilityCatalog.normalizeCapabilityIds(
                buildList {
                    addAll(
                        withDirection.filterNot { capabilityId ->
                            (capabilityId == "connections" && systemRemainingCapabilityState.connectionsEstablished) ||
                                (capabilityId == "inbox_sorting" && systemRemainingCapabilityState.inboxSortingEstablished) ||
                                (capabilityId == "key_problems" && systemRemainingCapabilityState.keyProblemsEstablished)
                        },
                    )
                    if (
                        systemRemainingCapabilityState.inboxSortingEstablished &&
                        systemRemainingCapabilityState.inboxSortingEnabled
                    ) {
                        add("inbox_sorting")
                    }
                    if (
                        systemRemainingCapabilityState.keyProblemsEstablished &&
                        systemRemainingCapabilityState.keyProblemsEnabled
                    ) {
                        add("key_problems")
                    }
                },
            )
        }
    return if (systemBacklogLifecycleState?.isEstablished == true) {
        withRemaining.filterNot { it == "backlog" }
    } else {
        withRemaining
    }
}

private fun Goal.toSharedBacklogItem(contextId: String): SharedBacklogItem =
    SharedBacklogItem(
        id = id,
        contextId = contextId,
        title = text,
        details = description,
        kind = SharedBacklogItemKind.Goal,
        priority = goalStatus.toSharedPriority(),
        isDone = completed,
    )

private fun SharedBacklogPriority.toGoalStatus(completed: Boolean = false): String =
    when {
        completed -> GoalStatusValues.DONE
        this == SharedBacklogPriority.Critical || this == SharedBacklogPriority.High -> GoalStatusValues.IN_WORK
        else -> GoalStatusValues.ACTIVE
    }

private fun String.toSharedPriority(): SharedBacklogPriority =
    when (this) {
        GoalStatusValues.DONE, GoalStatusValues.IN_WORK -> SharedBacklogPriority.High
        GoalStatusValues.PAUSED, GoalStatusValues.UNSURE -> SharedBacklogPriority.Medium
        else -> SharedBacklogPriority.Low
    }

private val LEGACY_ANDROID_CAPABILITY_IDS =
    setOf(
        "inbox",
        "log",
        "dashboard",
        "backlog",
        "connections",
    )
