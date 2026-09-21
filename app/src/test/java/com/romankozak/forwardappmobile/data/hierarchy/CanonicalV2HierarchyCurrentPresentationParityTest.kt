package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyPresentationTreeBuilder
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationBeaconInput
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationHierarchyBuilder
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbsToContext
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2HierarchyCurrentPresentationParityTest {
    @Test
    fun `V2 presentation matches CURRENT groups links duplicate workspace focus and breadcrumbs`() {
        val presentations =
            listOf(
                presentation("owner", null, 0),
                presentation("owner-child", "owner", 0),
                presentation("extra-root", null, 1),
                presentation("shared", "extra-root", 0),
                presentation("free-root", null, 2),
                presentation("free-child", "free-root", 0),
            )
        val workspaces =
            presentations.map { workspace(it.id, it.parentId, it.order) }
        val contextLinks =
            listOf(
                ContextParentLink(
                    parentContextId = "owner",
                    childContextId = "shared",
                    order = 0,
                ),
            )
        val beaconLinks =
            listOf(
                MainBeaconParentLink(
                    parentBeaconId = "b2",
                    childBeaconId = "b-child",
                    order = 0,
                ),
            )
        val groups =
            listOf(
                MainBeaconGroup(
                    id = "group",
                    title = "Core",
                    order = 0,
                ),
            )
        val beacons =
            listOf(
                beacon("b1", 0, null, listOf("owner"), listOf("group")),
                beacon("b2", 1, null),
                beacon("b-child", 0, "b1"),
            )

        val current =
            OrientationHierarchyBuilder().build(
                presentationHierarchy =
                    HierarchyPresentationTreeBuilder().build(presentations),
                beacons = beacons,
                groups = groups,
                parentLinks = contextLinks,
                beaconParentLinks = beaconLinks,
                workspaces = workspaces,
            )

        val snapshot =
            snapshot(
                presentations = presentations,
                beacons = beacons,
                groups = groups,
                contextLinks = contextLinks,
                beaconLinks = beaconLinks,
            )

        val projection =
            CanonicalV2HierarchyProjector().project(
                placements = placements(snapshot),
                admittedTargets =
                    targetPresentations(
                        snapshot = snapshot,
                        workspacePresentations = presentations,
                        beacons = beacons,
                    ),
            )

        val v2 =
            CanonicalV2HierarchySyntheticPresentationComposer().compose(
                hierarchy = projection,
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.GROUP,
                            id = "group",
                            title = "Core",
                            order = 0,
                            rootTargets = listOf(subjectTarget("b1")),
                        ),
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(subjectTarget("b2")),
                        ),
                    ),
            )

        assertEquals(
            current.map(::normalizeCurrent),
            v2.entries.map(::normalizeV2),
        )

        val currentShared =
            current.filter {
                (it.node as? OrientationHierarchyNode.ProjectLike)?.id == "shared"
            }
        val v2Shared =
            v2.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .filter {
                    it.target.type == HierarchyTargetType.WORKSPACE &&
                        it.target.id == "shared"
                }

        assertEquals(2, currentShared.size)
        assertEquals(2, v2Shared.size)

        assertEquals(
            current.indexOfFirst {
                (it.node as? OrientationHierarchyNode.ProjectLike)?.id == "shared"
            },
            v2.entries.indexOfFirst {
                it is CanonicalV2PresentedHierarchyEntry.Occurrence &&
                    it.target.type == HierarchyTargetType.WORKSPACE &&
                    it.target.id == "shared"
            },
        )

        val currentBreadcrumbs =
            buildOrientationBreadcrumbsToContext(current, "shared")
                .map {
                    BreadcrumbParity(
                        id = it.id,
                        title = it.name,
                        level = it.level,
                        target = normalizeBreadcrumbTarget(it.target.toString()),
                    )
                }
        val v2Breadcrumbs =
            v2.breadcrumbsToWorkspace("shared")
                .map {
                    BreadcrumbParity(
                        id = it.id,
                        title = it.title,
                        level = it.level,
                        target = normalizeBreadcrumbTarget(it.target.toString()),
                    )
                }

        assertEquals(currentBreadcrumbs, v2Breadcrumbs)

        val currentOwner =
            current
                .map { it.node }
                .filterIsInstance<OrientationHierarchyNode.ProjectLike>()
                .first { it.id == "owner" }
        val currentOwnerChild =
            current
                .map { it.node }
                .filterIsInstance<OrientationHierarchyNode.ProjectLike>()
                .first { it.id == "owner-child" }
        val currentFirstShared =
            current
                .map { it.node }
                .filterIsInstance<OrientationHierarchyNode.ProjectLike>()
                .first { it.id == "shared" }

        assertTrue(currentOwner.isLinkedAppearance)
        assertEquals(false, currentOwnerChild.isLinkedAppearance)
        assertTrue(currentFirstShared.isLinkedAppearance)

        val ownerChildOccurrence =
            v2.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .first {
                    it.target.type == HierarchyTargetType.WORKSPACE &&
                        it.target.id == "owner-child"
                }
        assertEquals(
            PlacementKind.LINK,
            requireNotNull(
                projection.occurrence(ownerChildOccurrence.placementId),
            ).placementKind,
        )
        // CURRENT isLinkedAppearance describes the legacy rendering edge
        // source. PlacementKind describes durable occurrence semantics.
        // A canonical child below a linked owner is therefore CURRENT=false
        // while its H2/V2 occurrence may correctly remain LINK.
    }
    @Test
    fun `shell free System Inbox keeps CURRENT presentation and V2 placement path`() {
        val parent = presentation("ordinary-parent", null, 0)
        val inbox =
            presentation(
                id = SystemContexts.INBOX.raw,
                parentId = parent.id,
                order = 0,
                name = "Canonical inbox",
            )
        val presentations = listOf(parent, inbox)
        val workspaces =
            presentations.map { workspace(it.id, it.parentId, it.order) }

        val current =
            OrientationHierarchyBuilder().build(
                presentationHierarchy =
                    HierarchyPresentationTreeBuilder().build(presentations),
                beacons = emptyList(),
                groups = emptyList(),
                parentLinks = emptyList(),
                beaconParentLinks = emptyList(),
                workspaces = workspaces,
            )

        val snapshot =
            snapshot(
                presentations = presentations,
                beacons = emptyList(),
                groups = emptyList(),
                contextLinks = emptyList(),
                beaconLinks = emptyList(),
            )
        val projection =
            CanonicalV2HierarchyProjector().project(
                placements = placements(snapshot),
                admittedTargets =
                    targetPresentations(
                        snapshot = snapshot,
                        workspacePresentations = presentations,
                        beacons = emptyList(),
                    ),
            )
        val v2 =
            CanonicalV2HierarchySyntheticPresentationComposer().compose(
                hierarchy = projection,
                scopes = emptyList(),
            )

        assertEquals(
            current.map(::normalizeCurrent),
            v2.entries.map(::normalizeV2),
        )

        val currentBreadcrumbs =
            buildOrientationBreadcrumbsToContext(
                current,
                SystemContexts.INBOX.raw,
            ).map {
                BreadcrumbParity(
                    id = it.id,
                    title = it.name,
                    level = it.level,
                    target = normalizeBreadcrumbTarget(it.target.toString()),
                )
            }
        val v2Breadcrumbs =
            v2.breadcrumbsToWorkspace(SystemContexts.INBOX.raw)
                .map {
                    BreadcrumbParity(
                        id = it.id,
                        title = it.title,
                        level = it.level,
                        target = normalizeBreadcrumbTarget(it.target.toString()),
                    )
                }

        assertEquals(currentBreadcrumbs, v2Breadcrumbs)
        assertEquals(
            listOf(
                CANONICAL_V2_NO_BEACON_SCOPE_ID,
                parent.id,
                SystemContexts.INBOX.raw,
            ),
            v2.entries.map { it.id },
        )
    }

    private fun normalizeCurrent(
        item: OrientationHierarchyItem,
    ): PresentedParity =
        when (val node = item.node) {
            is OrientationHierarchyNode.Group ->
                PresentedParity("GROUP", node.id, node.title, item.level)

            is OrientationHierarchyNode.Beacon ->
                PresentedParity("BEACON", node.id, node.title, item.level)

            OrientationHierarchyNode.NoGroup ->
                PresentedParity("NO_GROUP", node.id, node.title, item.level)

            OrientationHierarchyNode.NoBeacon ->
                PresentedParity("NO_BEACON", node.id, node.title, item.level)

            is OrientationHierarchyNode.ProjectLike ->
                PresentedParity(
                    kind = "WORKSPACE",
                    id = node.id,
                    title = node.title,
                    level = item.level,
                )
        }

    private fun normalizeV2(
        entry: CanonicalV2PresentedHierarchyEntry,
    ): PresentedParity =
        when (entry) {
            is CanonicalV2PresentedHierarchyEntry.SyntheticScope ->
                PresentedParity(
                    kind =
                        when (entry.kind) {
                            CanonicalV2SyntheticScopeKind.GROUP -> "GROUP"
                            CanonicalV2SyntheticScopeKind.NO_GROUP -> "NO_GROUP"
                            CanonicalV2SyntheticScopeKind.NO_BEACON -> "NO_BEACON"
                        },
                    id = entry.id,
                    title = entry.title,
                    level = entry.level,
                )

            is CanonicalV2PresentedHierarchyEntry.Occurrence -> {
                PresentedParity(
                    kind =
                        when (entry.target.type) {
                            HierarchyTargetType.MANAGED_SUBJECT -> "BEACON"
                            HierarchyTargetType.WORKSPACE -> "WORKSPACE"
                        },
                    id = entry.id,
                    title = entry.title,
                    level = entry.level,
                )
            }
        }

    private fun snapshot(
        presentations: List<HierarchyContextPresentationNode>,
        beacons: List<OrientationBeaconInput>,
        groups: List<MainBeaconGroup>,
        contextLinks: List<ContextParentLink>,
        beaconLinks: List<MainBeaconParentLink>,
    ): CanonicalV1HierarchySnapshot =
        CanonicalV1HierarchySnapshotBuilder().build(
            CanonicalV1HierarchySnapshotInput(
                workspaces =
                    presentations.mapIndexed { index, presentation ->
                        CanonicalV1WorkspaceSnapshotInput(
                            id = presentation.id,
                            name = presentation.name,
                            parentWorkspaceId = presentation.parentId,
                            order = presentation.order,
                            sourceOrdinal = index,
                        )
                    },
                beacons =
                    beacons.mapIndexed { index, beacon ->
                        CanonicalV1BeaconSnapshotInput(
                            legacyBeaconId = beacon.id,
                            target = subjectTarget(beacon.id),
                            title = beacon.title,
                            order = beacon.order,
                            parentBeaconId = beacon.parentBeaconId,
                            relatedOwnerIds = beacon.relatedOwnerIds,
                            groupIds = beacon.groupIds,
                            groupOrders = beacon.groupOrders,
                            sourceOrdinal = index,
                        )
                    },
                groups =
                    groups.mapIndexed { index, group ->
                        CanonicalV1BeaconGroupSnapshotInput(
                            id = group.id,
                            title = group.title,
                            order = group.order,
                            canonicalSubjectId = "test-group-subject:${group.id}",
                            sourceOrdinal = index,
                        )
                    },
                contextParentLinks =
                    contextLinks.mapIndexed { index, link ->
                        CanonicalV1ContextParentLinkSnapshotInput(
                            parentWorkspaceId = link.parentContextId,
                            childWorkspaceId = link.childContextId,
                            order = link.order,
                            sourceOrdinal = index,
                        )
                    },
                beaconParentLinks =
                    beaconLinks.mapIndexed { index, link ->
                        CanonicalV1BeaconParentLinkSnapshotInput(
                            parentBeaconId = link.parentBeaconId,
                            childBeaconId = link.childBeaconId,
                            order = link.order,
                            sourceOrdinal = index,
                        )
                    },
            ),
        )

    private fun placements(
        snapshot: CanonicalV1HierarchySnapshot,
    ): List<HierarchyPlacement> =
        snapshot.occurrences.map { occurrence ->
            HierarchyPlacement(
                id = placementId(snapshot, occurrence.occurrenceKey),
                hierarchyId = snapshot.hierarchyId,
                target = occurrence.target,
                parentPlacementId =
                    occurrence.parentOccurrenceKey?.let {
                        placementId(snapshot, it)
                    },
                placementKind = occurrence.placementKind,
                siblingOrder = occurrence.siblingOrder,
                createdAt = 1,
                updatedAt = 1,
                syncedAt = null,
                isDeleted = false,
                version = 1,
            )
        }

    private fun placementId(
        snapshot: CanonicalV1HierarchySnapshot,
        occurrenceKey: String,
    ): PlacementId =
        CanonicalV1HierarchyMaterializer.deterministicPlacementId(
            hierarchyId = snapshot.hierarchyId.value,
            occurrenceKey = occurrenceKey,
        )

    private fun targetPresentations(
        snapshot: CanonicalV1HierarchySnapshot,
        workspacePresentations: List<HierarchyContextPresentationNode>,
        beacons: List<OrientationBeaconInput>,
    ): Map<HierarchyTargetRef, CanonicalV2HierarchyTargetPresentation> {
        val workspacesById = workspacePresentations.associateBy { it.id }
        val beaconsById = beacons.associateBy { it.id }

        return snapshot.occurrences
            .map { it.target }
            .distinct()
            .associateWith { target ->
                when (target.type) {
                    HierarchyTargetType.WORKSPACE -> {
                        val presentation = requireNotNull(workspacesById[target.id])
                        CanonicalV2HierarchyTargetPresentation(
                            target = target,
                            title = presentation.name,
                        )
                    }

                    HierarchyTargetType.MANAGED_SUBJECT -> {
                        val beacon = requireNotNull(beaconsById[target.id])
                        CanonicalV2HierarchyTargetPresentation(
                            target = target,
                            title = beacon.title,
                            presentationId = beacon.id,
                        )
                    }
                }
            }
    }

    private fun presentation(
        id: String,
        parentId: String?,
        order: Long,
        name: String = id,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = null,
        parentId = parentId,
        order = order,
        roleCode = null,
        tags = emptyList(),
    )

    private fun workspace(
        id: String,
        parentId: String?,
        order: Long,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = parentId,
        roleCode = null,
        workspaceOrder = order,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
        provenance = "CANONICAL_ONLY",
        sourceContextId = null,
    )

    private fun beacon(
        id: String,
        order: Long,
        parentId: String?,
        owners: List<String> = emptyList(),
        groups: List<String> = emptyList(),
    ) = OrientationBeaconInput(
        id = id,
        title = id,
        order = order,
        readinessStatus = MainBeaconReadinessStatus.READY,
        parentBeaconId = parentId,
        relatedOwnerIds = owners,
        groupIds = groups,
        groupOrders = groups.associateWith { 0L },
    )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.MANAGED_SUBJECT,
            id = id,
        )

    private fun normalizeBreadcrumbTarget(value: String): String =
        value.lowercase().replace("_", "")

    private data class PresentedParity(
        val kind: String,
        val id: String,
        val title: String,
        val level: Int,
    )

    private data class BreadcrumbParity(
        val id: String,
        val title: String,
        val level: Int,
        val target: String,
    )
}
