package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import javax.inject.Inject

/**
 * Non-structural screen metadata joined onto the occurrence-native V2 tree.
 *
 * None of these maps may contribute parentage, sibling order, occurrence
 * identity or synthetic-scope assignment.
 */
data class CanonicalV2HierarchyScreenMetadata(
    val workspacePresentationsByTargetId: Map<String, HierarchyContextPresentationNode>,
    val linkedBeaconIdsByWorkspaceTargetId: Map<String, Set<String>> = emptyMap(),
    val beaconsByPresentationId: Map<String, CanonicalV2HierarchyScreenBeaconMetadata> = emptyMap(),
    val groupsBySyntheticScopeId: Map<String, CanonicalV2HierarchyScreenGroupMetadata> = emptyMap(),
)

data class CanonicalV2HierarchyScreenBeaconMetadata(
    val readinessStatus: MainBeaconReadinessStatus,
    val relatedOwnerCount: Int,
)

data class CanonicalV2HierarchyScreenGroupMetadata(
    val beaconCount: Int,
)

/**
 * Pure V2 -> existing hierarchy-screen presentation boundary.
 *
 * Structural shape, ordering, depth and exact concrete-row identity come only
 * from [CanonicalV2ProductionHierarchyRead]. Legacy/UI metadata is a display
 * join and cannot alter topology.
 */
class CanonicalV2HierarchyScreenPresentationAdapter
    @Inject
    constructor() {
    fun adapt(
        read: CanonicalV2ProductionHierarchyRead,
        metadata: CanonicalV2HierarchyScreenMetadata,
    ): List<OrientationHierarchyItem> =
        read.presentation.entries.map { entry ->
            when (entry) {
                is CanonicalV2PresentedHierarchyEntry.SyntheticScope ->
                    OrientationHierarchyItem(
                        node = syntheticNode(entry, metadata),
                        level = entry.level,
                    )

                is CanonicalV2PresentedHierarchyEntry.Occurrence ->
                    OrientationHierarchyItem(
                        node = occurrenceNode(entry, read, metadata),
                        level = entry.level,
                    )
            }
        }

    private fun syntheticNode(
        entry: CanonicalV2PresentedHierarchyEntry.SyntheticScope,
        metadata: CanonicalV2HierarchyScreenMetadata,
    ): OrientationHierarchyNode =
        when (entry.kind) {
            CanonicalV2SyntheticScopeKind.GROUP -> {
                val group =
                    requireNotNull(metadata.groupsBySyntheticScopeId[entry.id]) {
                        "Missing screen metadata for V2 Group scope ${entry.id}"
                    }
                OrientationHierarchyNode.Group(
                    id = entry.id,
                    title = entry.title,
                    beaconCount = group.beaconCount,
                )
            }

            CanonicalV2SyntheticScopeKind.NO_GROUP ->
                OrientationHierarchyNode.NoGroup

            CanonicalV2SyntheticScopeKind.NO_BEACON ->
                OrientationHierarchyNode.NoBeacon
        }

    private fun occurrenceNode(
        entry: CanonicalV2PresentedHierarchyEntry.Occurrence,
        read: CanonicalV2ProductionHierarchyRead,
        metadata: CanonicalV2HierarchyScreenMetadata,
    ): OrientationHierarchyNode =
        when (entry.target.type) {
            HierarchyTargetType.WORKSPACE -> {
                val presentation =
                    requireNotNull(
                        metadata.workspacePresentationsByTargetId[entry.target.id],
                    ) {
                        "Missing screen metadata for V2 Workspace target ${entry.target.id}"
                    }
                require(presentation.id == entry.presentationId) {
                    "Workspace presentation identity mismatch for ${entry.placementId.value}: " +
                        "${presentation.id} != ${entry.presentationId}"
                }
                OrientationHierarchyNode.WorkspaceNode(
                    presentation = presentation,
                    linkedBeaconIds =
                        metadata.linkedBeaconIdsByWorkspaceTargetId[entry.target.id].orEmpty(),
                    isLinkedAppearance = read.isLinkedAppearance(entry.placementId),
                    placementId = entry.placementId,
                )
            }

            HierarchyTargetType.MANAGED_SUBJECT -> {
                val beacon =
                    requireNotNull(metadata.beaconsByPresentationId[entry.presentationId]) {
                        "Missing screen metadata for V2 Beacon presentation ${entry.presentationId}"
                    }
                OrientationHierarchyNode.Beacon(
                    id = entry.presentationId,
                    title = entry.title,
                    readinessStatus = beacon.readinessStatus,
                    relatedContextCount = beacon.relatedOwnerCount,
                    placementId = entry.placementId,
                )
            }
        }
}
