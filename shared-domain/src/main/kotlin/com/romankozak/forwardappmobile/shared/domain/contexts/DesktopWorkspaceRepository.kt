package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask

interface DesktopWorkspaceRepository {
    suspend fun getContexts(): List<SharedContextSummary>

    suspend fun createContext(
        parentId: String?,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String> = emptyList(),
        experimentalCapabilityIds: List<String> = emptyList(),
    ): SharedContextSummary?

    suspend fun updateContext(
        contextId: String,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String> = emptyList(),
        experimentalCapabilityIds: List<String> = emptyList(),
    ): SharedContextSummary?

    suspend fun deleteContext(contextId: String): Boolean

    suspend fun getBacklogItems(contextId: String): List<SharedBacklogItem>

    suspend fun getDayPlans(): List<SharedDayPlan> = emptyList()

    suspend fun getDayTasks(): List<SharedDayTask> = emptyList()

    suspend fun createBacklogItem(
        contextId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem?

    suspend fun updateBacklogItemDone(
        itemId: String,
        isDone: Boolean,
    ): SharedBacklogItem?

    suspend fun updateBacklogItemContent(
        itemId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem?

    suspend fun deleteBacklogItem(itemId: String): Boolean
}
