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
    val planningFormatVersion: Int? = null,
    val planningScopes: List<SharedPlanningScopeSnapshot>? = null,
    val planningCommitments: List<SharedPlanningCommitmentSnapshot>? = null,
    val dayPlanningScopeExtensions: List<SharedDayPlanningScopeExtensionSnapshot>? = null,
    val tacticalPlanningScopeExtensions: List<SharedTacticalPlanningScopeExtensionSnapshot>? = null,
    val strategicPlanningScopeExtensions: List<SharedStrategicPlanningScopeExtensionSnapshot>? = null,
)
