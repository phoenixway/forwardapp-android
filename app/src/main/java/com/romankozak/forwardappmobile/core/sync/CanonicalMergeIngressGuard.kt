package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementIngressBoundary
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.hasLegacyGeneralHierarchyEvidence
import com.romankozak.forwardappmobile.core.data.models.sync.requireHierarchyPlacementIngress
import com.romankozak.forwardappmobile.core.data.models.sync.requireSupportedHierarchyFormat

/**
 * Ordinary merge/sync ingress.
 *
 * Production follows the single hierarchy authority seam. Under P2
 * V2_AUTHORITY, hierarchy-bearing ingress must satisfy canonical H1 authority
 * requirements and cannot fall back to legacy GENERAL topology.
 */
internal fun requireCanonicalMergeIngress(
    bundle: SnapshotBundle,
) {
    bundle.requireSupportedHierarchyFormat()

    val ordinaryContextIds =
        bundle.contexts
            .asSequence()
            .map { snapshot -> snapshot.id }
            .filterNot { id -> SystemContexts.isSystem(ContextId(id)) }
            .toList()

    require(ordinaryContextIds.isEmpty()) {
        "Canonical merge ingress refuses ordinary Context persistence after the Context Big Cut: " +
            ordinaryContextIds.joinToString()
    }

    val systemConfigurationIds =
        bundle.contextConfigurations
            .asSequence()
            .filter { snapshot -> SystemContexts.isSystem(ContextId(snapshot.contextId)) }
            .map { snapshot -> snapshot.id }
            .toSet()

    val ordinaryConfigurationContextIds =
        bundle.contextConfigurations
            .asSequence()
            .map { snapshot -> snapshot.contextId }
            .filterNot { contextId -> SystemContexts.isSystem(ContextId(contextId)) }
            .distinct()
            .toList()

    require(ordinaryConfigurationContextIds.isEmpty()) {
        "Canonical merge ingress refuses ordinary Context configuration persistence after the Context Big Cut: " +
            ordinaryConfigurationContextIds.joinToString()
    }

    val ordinaryStructureItemIds =
        bundle.projectStructureItems
            .asSequence()
            .filterNot { item -> item.contextStructureId in systemConfigurationIds }
            .map { item -> item.id }
            .toList()

    require(ordinaryStructureItemIds.isEmpty()) {
        "Canonical merge ingress refuses ordinary Context structure persistence after the Context Big Cut: " +
            ordinaryStructureItemIds.joinToString()
    }

    requireHierarchyPlacementIngress(
        boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
        canonicalH1Present = bundle.hierarchyPlacements != null,
        legacyHierarchyBearing = bundle.hasLegacyGeneralHierarchyEvidence(),
    )

    val hierarchyStreamsPresent =
        listOf(
            bundle.hierarchyPlacements,
            bundle.hierarchyPlacementGroupScopes,
            bundle.hierarchyPlacementLinkedAppearances,
        ).map { it != null }

    require(hierarchyStreamsPresent.distinct().size == 1) {
        "Canonical V2 hierarchy ingress must carry H1, GroupScope, and linked-appearance streams together"
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
