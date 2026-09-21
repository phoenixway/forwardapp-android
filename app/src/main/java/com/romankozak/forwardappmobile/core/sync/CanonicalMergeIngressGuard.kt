package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementIngressBoundary
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.currentHierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.hasLegacyGeneralHierarchyEvidence
import com.romankozak.forwardappmobile.core.data.models.sync.requireHierarchyPlacementIngress

/**
 * Ordinary merge/sync ingress.
 *
 * Default remains CURRENT_PRE_CUTOVER. P2 must explicitly change this only
 * together with the coherent reader/writer authority cutover.
 */
internal fun requireCanonicalMergeIngress(
    bundle: SnapshotBundle,
    hierarchyAuthorityMode: HierarchyPlacementAuthorityMode =
        currentHierarchyPlacementAuthorityMode(),
) {
    requireHierarchyPlacementIngress(
        boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
        authorityMode = hierarchyAuthorityMode,
        canonicalH1Present = bundle.hierarchyPlacements != null,
        legacyHierarchyBearing = bundle.hasLegacyGeneralHierarchyEvidence(),
    )

    if (hierarchyAuthorityMode == HierarchyPlacementAuthorityMode.V2_AUTHORITY) {
        val hierarchyStreamsPresent =
            listOf(
                bundle.hierarchyPlacements,
                bundle.hierarchyPlacementGroupScopes,
                bundle.hierarchyPlacementLinkedAppearances,
            ).map { it != null }

        require(hierarchyStreamsPresent.distinct().size == 1) {
            "Canonical V2 hierarchy ingress must carry H1, GroupScope, and linked-appearance streams together"
        }
    }

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
        bundle.workspaceBacklogEntries != null || !hasOrdinaryLegacyBacklog,
    ) {
        "Merge rejected restore-only legacy BACKLOG content; use Restore/Replace"
    }

    val hasOrdinaryLegacyInbox =
        bundle.inbox.any { !SystemContexts.isSystem(ContextId(it.contextId)) }
    require(bundle.workspaceInboxRecords != null || !hasOrdinaryLegacyInbox) {
        "Merge rejected restore-only legacy INBOX content; use Restore/Replace"
    }
}
