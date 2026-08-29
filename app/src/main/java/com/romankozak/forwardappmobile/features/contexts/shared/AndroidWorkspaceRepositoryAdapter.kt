package com.romankozak.forwardappmobile.features.contexts.shared

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
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
) : DesktopWorkspaceRepository {
    override suspend fun getContexts(): List<SharedContextSummary> =
        contextRepository.getAllContextsFlow()
            .first()
            .filterNot { context -> context.isDeleted }
            .sortedBy { context -> context.order }
            .map { context ->
                context.toSharedSummary(
                    configuration = contextStructureRepository.getStructureByContext(context.id),
                )
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
        val created = requireNotNull(contextRepository.getContextById(contextId))
        val updated =
            created.copy(
                description = description,
                contextStatus = status.toAndroidStatus(),
                defaultViewModeName = defaultView.toAndroidViewMode().name,
                isCompleted = status == SharedContextStatus.Completed,
            )
        contextRepository.updateContext(updated)
        val configuration =
            upsertContextConfiguration(
                contextId = contextId,
                defaultView = defaultView,
                enabledCapabilityIds = enabledCapabilityIds,
                experimentalCapabilityIds = experimentalCapabilityIds,
            )
        return requireNotNull(contextRepository.getContextById(contextId)).toSharedSummary(configuration)
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
        val current = contextRepository.getContextById(contextId) ?: return null
        contextRepository.updateContext(
            current.copy(
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
        return contextRepository.getContextById(contextId)?.toSharedSummary(configuration)
    }

    override suspend fun deleteContext(contextId: String): Boolean {
        val current = contextRepository.getContextById(contextId) ?: return false
        contextRepository.deleteContextsAndSubContexts(listOf(current))
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
            SharedContextCapabilityCatalog.normalizeCapabilityIds(
                enabledCapabilityIds +
                    experimentalCapabilityIds +
                    SharedContextCapabilityCatalog.defaultCapabilityIdsFor(defaultView),
            )
        val current = contextStructureRepository.getStructureByContext(contextId) ?: ContextConfiguration.default(contextId)
        val updated =
            current.copy(
                enableInbox = capabilityIds.contains("inbox"),
                enableLog = capabilityIds.contains("log"),
                enableArtifact = capabilityIds.contains("artifact"),
                enableDashboard = capabilityIds.contains("dashboard"),
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
        return updated
    }
}

private fun Context.toSharedSummary(configuration: ContextConfiguration?): SharedContextSummary {
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
        enabledCapabilityIds = configuration.enabledCapabilityIds(defaultView),
        experimentalCapabilityIds = configuration.experimentalCapabilityIds(),
    )
}

private fun ContextConfiguration?.enabledCapabilityIds(defaultView: SharedContextView): List<String> {
    val explicitIds =
        buildList {
            if (this@enabledCapabilityIds?.enableInbox == true) add("inbox")
            if (this@enabledCapabilityIds?.enableLog == true) add("log")
            if (this@enabledCapabilityIds?.enableArtifact == true) add("artifact")
            if (this@enabledCapabilityIds?.enableDashboard == true) add("dashboard")
            if (this@enabledCapabilityIds?.enableBacklog == true) add("backlog")
            if (this@enabledCapabilityIds?.enableAttachments == true) add("connections")
        }
    val fallbackIds =
        if (explicitIds.isEmpty()) {
            SharedContextCapabilityCatalog.defaultCapabilityIdsFor(defaultView)
        } else {
            emptyList()
        }
    return SharedContextCapabilityCatalog.normalizeCapabilityIds(
        explicitIds + fallbackIds + SharedContextCapabilityCatalog.capabilityIdFor(defaultView),
    )
}

private fun ContextConfiguration?.experimentalCapabilityIds(): List<String> =
    SharedContextCapabilityCatalog.normalizeCapabilityIds(
        this
            ?.experimentalCapabilityIds
            .orEmpty()
            .map { capabilityId -> capabilityId.raw },
    )

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
        ContextViewMode.JOURNAL_LOG.name -> SharedContextView.JournalLog
        ContextViewMode.ARTIFACT.name -> SharedContextView.Artifact
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
        SharedContextView.JournalLog -> ContextViewMode.JOURNAL_LOG
        SharedContextView.Artifact -> ContextViewMode.ARTIFACT
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
        "artifact",
        "dashboard",
        "backlog",
        "connections",
    )
