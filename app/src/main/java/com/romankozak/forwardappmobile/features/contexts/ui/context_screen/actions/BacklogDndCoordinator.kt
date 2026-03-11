package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent

class BacklogDndCoordinator(
    private val backlogActions: BacklogActions,
    private val logWarning: (String, Throwable?) -> Unit,
) {
    private var isDragInProgress: Boolean = false
    private var expectedOrderIds: List<String>? = null

    fun reset() {
        isDragInProgress = false
        expectedOrderIds = null
    }

    fun applyObserved(
        observed: List<BacklogItemContent>,
        current: List<BacklogItemContent>,
    ): List<BacklogItemContent> {
        if (isDragInProgress) return current

        val expectedOrder = expectedOrderIds
        if (expectedOrder != null) {
            val observedOrder = observed.map { it.backlogItem.id }
            if (observedOrder != expectedOrder) {
                return current
            }
            expectedOrderIds = null
        }

        return observed
    }

    fun move(
        current: List<BacklogItemContent>,
        from: Int,
        to: Int,
    ): List<BacklogItemContent> {
        if (!isDragInProgress) {
            isDragInProgress = true
            expectedOrderIds = null
        }
        return backlogActions.moveInMemory(current, from, to)
    }

    suspend fun onDragStopped(current: List<BacklogItemContent>) {
        expectedOrderIds = current.map { it.backlogItem.id }
        isDragInProgress = false

        runCatching {
            backlogActions.persistBacklogOrder(current)
        }.onFailure { error ->
            logWarning("Failed to persist backlog order after drag", error)
            expectedOrderIds = null
        }
    }
}
