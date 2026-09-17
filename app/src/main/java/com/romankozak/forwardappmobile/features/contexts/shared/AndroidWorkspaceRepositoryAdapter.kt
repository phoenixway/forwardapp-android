package com.romankozak.forwardappmobile.features.contexts.shared

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextSharedStateUpdate
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.SystemRemainingCapabilityLifecycleState
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import java.util.UUID
import kotlinx.coroutines.flow.first

class AndroidWorkspaceRepositoryAdapter(
    private val contextRepository: ContextRepository,
    private val goalRepository: GoalRepository,
    private val contextStructureRepository: ContextStructureRepository,
    private val canonicalWorkspaceBootstrapper: CanonicalWorkspaceBootstrapper,
    private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
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
        val rawById = rawContexts.associateBy { context -> context.id }

        return systemWorkspacePresentationContextProjector
            .projectPresentationUniverse(rawContexts)
            .sortedBy { presentation -> presentation.order }
            .map { presentation ->
                toSharedSummary(
                    presentation = presentation,
                    rawContext = rawById[presentation.id],
                    configuration = contextStructureRepository.getStructureByContext(presentation.id),
                )
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
        val contextId = UUID.randomUUID().toString()
        contextRepository.createContextWithId(
            id = contextId,
            name = name,
            parentId = parentId,
        )
        contextRepository.updateContextSharedState(
            contextId = contextId,
            update =
                ContextSharedStateUpdate(
                    name = name,
                    description = description,
                    contextStatus = status.toAndroidStatus(),
                    defaultViewModeName = defaultView.toAndroidViewMode().name,
                    isCompleted = status == SharedContextStatus.Completed,
                ),
        )
        val configuration =
            upsertContextConfiguration(
                contextId = contextId,
                defaultView = defaultView,
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )
        val requestedCapabilities =
            requestedCapabilityIds(
                defaultView = defaultView,
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )
        canonicalDashboardCapabilityRepository.setEnabled(
            workspaceId = contextId,
            enabled = requestedCapabilities.contains("dashboard"),
        )
        canonicalExecutionLogRepository.setEnabled(
            workspaceId = contextId,
            enabled = requestedCapabilities.contains("log"),
        )
        return toSharedSummary(
            context = requireNotNull(contextRepository.getContextById(contextId)),
            configuration = configuration,
        )
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

        val rawBefore = contextRepository.getContextById(contextId)
        val presentationBefore =
            systemWorkspacePresentationContextProjector.resolvePresentation(
                contextId = contextId,
                context = rawBefore,
            ) ?: return null
        val isReservedSystem = SystemContexts.isSystem(ContextId(contextId))

        val requestedCapabilities =
            requestedCapabilityIds(
                defaultView = defaultView,
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )

        if (isReservedSystem) {
            if (systemInboxDirectionAccess.handles(contextId)) {
                systemInboxDirectionAccess.setInboxEnabled(
                    contextId = contextId,
                    enabled = requestedCapabilities.contains("inbox"),
                )
                systemInboxDirectionAccess.setDirectionEnabled(
                    contextId = contextId,
                    enabled = requestedCapabilities.contains("direction"),
                )
            }
            if (systemRemainingCapabilityAccess.handles(contextId)) {
                systemRemainingCapabilityAccess.setConnectionsEnabled(
                    contextId = contextId,
                    enabled = requestedCapabilities.contains("connections"),
                )
                systemRemainingCapabilityAccess.setInboxSortingEnabled(
                    contextId = contextId,
                    enabled = requestedCapabilities.contains("inbox_sorting"),
                )
                systemRemainingCapabilityAccess.setKeyProblemsEnabled(
                    contextId = contextId,
                    enabled = requestedCapabilities.contains("key_problems"),
                )
            }
            if (systemBacklogLifecycleAccess.handles(contextId)) {
                systemBacklogLifecycleAccess.setEnabled(
                    contextId = contextId,
                    enabled = requestedCapabilities.contains("backlog"),
                )
            }

            contextRepository.updateContextPresentation(
                contextId = contextId,
                name = name,
                description = description,
            )

            // context_structures has no FK to Context, but it is not promoted
            // System write authority. Keep any existing row read-only here.
            canonicalDashboardCapabilityRepository.setEnabled(
                workspaceId = contextId,
                enabled = requestedCapabilities.contains("dashboard"),
            )
            canonicalExecutionLogRepository.setEnabled(
                workspaceId = contextId,
                enabled = requestedCapabilities.contains("log"),
            )

            val presented =
                systemWorkspacePresentationContextProjector.resolvePresentation(
                    contextId = contextId,
                    context = contextRepository.getContextById(contextId),
                ) ?: return null

            return toSharedSummary(
                presentation = presented,
                rawContext = null,
                configuration = contextStructureRepository.getStructureByContext(contextId),
            )
        }

        rawBefore ?: return null
        contextRepository.updateContextSharedState(
            contextId = contextId,
            update =
                ContextSharedStateUpdate(
                    name = name,
                    description = description,
                    contextStatus = status.toAndroidStatus(),
                    defaultViewModeName = defaultView.toAndroidViewMode().name,
                    isCompleted = status == SharedContextStatus.Completed,
                ),
        )
        val configuration =
            upsertContextConfiguration(
                contextId = contextId,
                defaultView = defaultView,
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )
        canonicalDashboardCapabilityRepository.setEnabled(
            workspaceId = contextId,
            enabled = requestedCapabilities.contains("dashboard"),
        )
        canonicalExecutionLogRepository.setEnabled(
            workspaceId = contextId,
            enabled = requestedCapabilities.contains("log"),
        )

        val updated = contextRepository.getContextById(contextId) ?: return null
        val presented =
            systemWorkspacePresentationContextProjector.resolvePresentation(
                contextId = contextId,
                context = updated,
            ) ?: return null
        return toSharedSummary(
            presentation = presented,
            rawContext = updated,
            configuration = configuration,
        )
    }

    override suspend fun deleteContext(contextId: String): Boolean {
        if (SystemContexts.isSystem(ContextId(contextId))) return false
        val current = contextRepository.getContextById(contextId) ?: return false
        contextRepository.deleteContextsByIds(listOf(current.id))
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

    private suspend fun upsertContextConfiguration(
        contextId: String,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String>,
        experimentalCapabilityIds: List<String>,
    ): ContextConfiguration {
        val capabilityIds =
            requestedCapabilityIds(
                defaultView = defaultView,
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )
        val current = contextStructureRepository.getStructureByContext(contextId) ?: ContextConfiguration.default(contextId)
        val updated =
            current.copy(
                enableInbox = capabilityIds.contains("inbox"),
                enableBacklog = capabilityIds.contains("backlog"),
                enableAttachments = capabilityIds.contains("connections"),
                experimentalCapabilityIds =
                    capabilityIds
                        .filterNot { capabilityId -> capabilityId in LEGACY_ANDROID_CAPABILITY_IDS }
                        .map(::CapabilityId),
                updatedAt = System.currentTimeMillis(),
                version = current.version + 1,
                isDeleted = false,
            )
        contextStructureRepository.upsertStructure(updated)
        return requireNotNull(contextStructureRepository.getStructureByContext(contextId)) {
            "Context configuration disappeared after persistence: $contextId"
        }
    }

    private suspend fun toSharedSummary(
        context: Context,
        configuration: ContextConfiguration?,
    ): SharedContextSummary {
        val systemState = systemInboxDirectionAccess.getState(context.id)
        val remainingSystemState = systemRemainingCapabilityAccess.getState(context.id)
        val systemBacklogState = systemBacklogLifecycleAccess.getState(context.id)
        return context.toSharedSummary(
            configuration = configuration,
            dashboardEnabled = canonicalDashboardCapabilityRepository.isEnabled(context.id),
            executionLogEnabled = canonicalExecutionLogRepository.isEnabled(context.id),
            systemInboxDirectionState = systemState,
            systemRemainingCapabilityState = remainingSystemState,
            systemBacklogLifecycleState = systemBacklogState,
        )
    }

    private suspend fun toSharedSummary(
        presentation: ContextPresentation,
        rawContext: Context?,
        configuration: ContextConfiguration?,
    ): SharedContextSummary {
        val systemState = systemInboxDirectionAccess.getState(presentation.id)
        val remainingSystemState = systemRemainingCapabilityAccess.getState(presentation.id)
        val systemBacklogState = systemBacklogLifecycleAccess.getState(presentation.id)

        val defaultView =
            rawContext?.defaultViewModeName.toSharedView()

        return SharedContextSummary(
            id = presentation.id,
            name = presentation.name,
            description = presentation.description,
            parentId = presentation.parentId,
            status = rawContext?.contextStatus.toSharedStatus(),
            defaultView = defaultView,
            score = rawContext?.displayScore ?: 0,
            isCompleted = rawContext?.isCompleted ?: false,
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


private fun Context.toSharedSummary(
    configuration: ContextConfiguration?,
    dashboardEnabled: Boolean,
    executionLogEnabled: Boolean,
    systemInboxDirectionState: SystemInboxDirectionState?,
    systemRemainingCapabilityState: SystemRemainingCapabilityLifecycleState?,
    systemBacklogLifecycleState: SystemBacklogLifecycleState?,
): SharedContextSummary {
    val defaultView = defaultViewModeName.toSharedView()
    return SharedContextSummary(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        status = contextStatus.toSharedStatus(),
        defaultView = defaultView,
        score = displayScore,
        isCompleted = isCompleted,
        enabledCapabilityIds =
            configuration.enabledCapabilityIds(
                defaultView = defaultView,
                dashboardEnabled = dashboardEnabled,
                executionLogEnabled = executionLogEnabled,
                systemInboxDirectionState = systemInboxDirectionState,
                systemRemainingCapabilityState = systemRemainingCapabilityState,
                systemBacklogLifecycleState = systemBacklogLifecycleState,
            ),
        experimentalCapabilityIds =
            configuration.experimentalCapabilityIds(
                systemInboxDirectionState,
                systemRemainingCapabilityState,
                systemBacklogLifecycleState,
            ),
    )
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
    defaultView: SharedContextView,
    enabledCapabilityIds: List<String>,
    experimentalCapabilityIds: List<String>,
): List<String> =
    SharedContextCapabilityCatalog.normalizeCapabilityIds(
        enabledCapabilityIds +
            experimentalCapabilityIds +
            SharedContextCapabilityCatalog.defaultCapabilityIdsFor(defaultView),
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

private fun String?.toSharedStatus(): SharedContextStatus =
    when (this) {
        ContextStatusValues.PLANNING -> SharedContextStatus.Planning
        ContextStatusValues.IN_PROGRESS -> SharedContextStatus.InProgress
        ContextStatusValues.COMPLETED -> SharedContextStatus.Completed
        ContextStatusValues.ON_HOLD -> SharedContextStatus.OnHold
        ContextStatusValues.PAUSED -> SharedContextStatus.Paused
        else -> SharedContextStatus.NoPlan
    }

private fun SharedContextStatus.toAndroidStatus(): String =
    when (this) {
        SharedContextStatus.Planning -> ContextStatusValues.PLANNING
        SharedContextStatus.InProgress -> ContextStatusValues.IN_PROGRESS
        SharedContextStatus.Completed -> ContextStatusValues.COMPLETED
        SharedContextStatus.OnHold -> ContextStatusValues.ON_HOLD
        SharedContextStatus.Paused -> ContextStatusValues.PAUSED
        SharedContextStatus.NoPlan -> ContextStatusValues.NO_PLAN
    }

private fun String?.toSharedView(): SharedContextView =
    when (this) {
        ContextViewMode.INBOX.name -> SharedContextView.Inbox
        ContextViewMode.CONNECTIONS.name -> SharedContextView.Connections
        ContextViewMode.DASHBOARD.name -> SharedContextView.Dashboard
        ContextViewMode.DIRECTION.name -> SharedContextView.Direction
        ContextViewMode.LOG.name -> SharedContextView.Log
        ContextViewMode.KEY_PROBLEMS.name -> SharedContextView.KeyProblems
        else -> SharedContextView.Backlog
    }

private fun SharedContextView.toAndroidViewMode(): ContextViewMode =
    when (this) {
        SharedContextView.Inbox -> ContextViewMode.INBOX
        SharedContextView.Connections -> ContextViewMode.CONNECTIONS
        SharedContextView.Dashboard -> ContextViewMode.DASHBOARD
        SharedContextView.Direction -> ContextViewMode.DIRECTION
        SharedContextView.Log -> ContextViewMode.LOG
        SharedContextView.KeyProblems -> ContextViewMode.KEY_PROBLEMS
        else -> ContextViewMode.BACKLOG
    }

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
