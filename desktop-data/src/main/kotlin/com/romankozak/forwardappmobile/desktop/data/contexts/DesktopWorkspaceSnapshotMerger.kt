package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata

class DesktopWorkspaceSnapshotMerger {
    fun merge(
        local: DesktopWorkspaceSnapshot,
        incoming: DesktopWorkspaceSnapshot,
    ): DesktopWorkspaceSnapshot =
        DesktopWorkspaceSnapshot(
            contexts = mergeById(local.contexts, incoming.contexts, SharedContextSummary::id, SharedContextSummary::sync),
            backlogItems = mergeById(local.backlogItems, incoming.backlogItems, SharedBacklogItem::id, SharedBacklogItem::sync),
            dayPlans = mergeById(local.dayPlans, incoming.dayPlans, SharedDayPlan::id, SharedDayPlan::sync),
            dayTasks = mergeById(local.dayTasks, incoming.dayTasks, SharedDayTask::id, SharedDayTask::sync),
        )

    private fun <T> mergeById(
        localItems: List<T>,
        incomingItems: List<T>,
        idSelector: (T) -> String,
        syncSelector: (T) -> SharedSyncMetadata,
    ): List<T> {
        val incomingById = incomingItems.associateBy(idSelector)
        val localById = localItems.associateBy(idSelector)
        val ids = linkedSetOf<String>().apply {
            addAll(localItems.map(idSelector))
            addAll(incomingItems.map(idSelector))
        }
        return ids.mapNotNull { id ->
            val local = localById[id]
            val incoming = incomingById[id]
            when {
                local == null -> incoming
                incoming == null -> local
                syncSelector(incoming).isNewerThan(syncSelector(local)) -> incoming
                else -> local
            }
        }
    }
}

private fun SharedSyncMetadata.isNewerThan(other: SharedSyncMetadata): Boolean =
    updatedAt > other.updatedAt || (updatedAt == other.updatedAt && version > other.version)
