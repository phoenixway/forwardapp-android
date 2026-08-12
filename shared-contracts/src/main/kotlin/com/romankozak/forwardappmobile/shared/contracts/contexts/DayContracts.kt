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
data class SharedDayTask(
    val id: String,
    val dayPlanId: String,
    val title: String,
    val description: String? = null,
    val projectId: String? = null,
    val linkedProjectIds: List<String> = emptyList(),
    val isDone: Boolean = false,
    val priority: String = "",
    val order: Long = Long.MAX_VALUE,
    val scheduledTime: Long? = null,
    val dueTime: Long? = null,
    val sync: SharedSyncMetadata = SharedSyncMetadata(),
)
