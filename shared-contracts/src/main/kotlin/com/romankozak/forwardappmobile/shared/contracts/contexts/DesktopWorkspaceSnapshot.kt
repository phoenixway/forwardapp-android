package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class DesktopWorkspaceSnapshot(
    val contexts: List<SharedContextSummary>,
    val backlogItems: List<SharedBacklogItem>,
    val dayPlans: List<SharedDayPlan> = emptyList(),
    val dayFocusItems: List<SharedDayFocusItem> = emptyList(),
    val dayTasks: List<SharedDayTask> = emptyList(),
    val recurringTasks: List<SharedRecurringTask> = emptyList(),
)
