package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class SharedDayPlan(
    val id: String,
    val date: Long,
    val name: String? = null,
    val status: String = "",
    val sync: SharedSyncMetadata = SharedSyncMetadata(),
)

@Serializable
data class SharedDayFocusItem(
    val id: String,
    val dayPlanId: String,
    val title: String,
    val notes: String? = null,
    val type: String = "FOCUS",
    val isEveryday: Boolean = false,
    val recurringKey: String? = null,
    val budgetPercent: Int? = null,
    val order: Long = Long.MAX_VALUE,
    val isDeleted: Boolean = false,
    val sync: SharedSyncMetadata = SharedSyncMetadata(),
)

@Serializable
data class SharedDayTask(
    val id: String,
    val dayPlanId: String,
    val title: String,
    val description: String? = null,
    val projectId: String? = null,
    val linkedProjectIds: List<String> = emptyList(),
    val recurringTaskId: String? = null,
    val taskType: String? = null,
    val isDone: Boolean = false,
    val priority: String = "",
    val order: Long = Long.MAX_VALUE,
    val scheduledTime: Long? = null,
    val estimatedDurationMinutes: Long? = null,
    val dueTime: Long? = null,
    val points: Int = 0,
    val isDeleted: Boolean = false,
    val sync: SharedSyncMetadata = SharedSyncMetadata(),
)

@Serializable
data class SharedRecurringTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val goalId: String? = null,
    val linkedProjectIds: List<String> = emptyList(),
    val duration: Int? = null,
    val priority: String = "MEDIUM",
    val points: Int = 0,
    val frequency: String = "DAILY",
    val daysOfWeek: List<String> = emptyList(),
    val startDate: Long,
    val endDate: Long? = null,
)
