package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class DesktopWorkspaceSnapshot(
    val contexts: List<SharedContextSummary>,
    val backlogItems: List<SharedBacklogItem>,
    val dayPlans: List<SharedDayPlan> = emptyList(),
    val dayTasks: List<SharedDayTask> = emptyList(),
)
