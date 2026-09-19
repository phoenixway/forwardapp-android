package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle

/** Ordinary Context-era BACKLOG/INBOX compatibility is Restore-only. */
internal fun requireCanonicalMergeIngress(bundle: SnapshotBundle) {
            require(
                bundle.workspaceCapabilityInstances != null ||
                    bundle.contextInboxSortingRules.isEmpty(),
            ) {
                "Legacy Context INBOX_SORTING policy is restore-only; use Restore/Replace instead of Merge"
            }

    val hasOrdinaryLegacyBacklog =
        bundle.backlogItems.any { !SystemContexts.isSystem(ContextId(it.contextId)) } ||
            bundle.backlogOrders.any { !SystemContexts.isSystem(ContextId(it.listId)) }
    require(
        bundle.workspaceBacklogEntries != null ||
            !hasOrdinaryLegacyBacklog,
    ) {
        "Merge rejected restore-only legacy BACKLOG content; use Restore/Replace"
    }
    val hasOrdinaryLegacyInbox =
        bundle.inbox.any { !SystemContexts.isSystem(ContextId(it.contextId)) }
    require(bundle.workspaceInboxRecords != null || !hasOrdinaryLegacyInbox) {
        "Merge rejected restore-only legacy INBOX content; use Restore/Replace"
    }
}
