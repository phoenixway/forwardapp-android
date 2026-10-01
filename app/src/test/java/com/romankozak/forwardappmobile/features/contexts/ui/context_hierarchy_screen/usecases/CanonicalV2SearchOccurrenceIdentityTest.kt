package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.hierarchy.CANONICAL_V2_NO_GROUP_SCOPE_ID
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyReadAdapter
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2SyntheticScopeInput
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2SyntheticScopeKind
import com.romankozak.forwardappmobile.data.hierarchy.toCanonicalV2WorkspacePresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalV2SearchOccurrenceIdentityTest {
    @Test
    fun duplicateWorkspaceAppearancesRemainDistinctSearchResultsWithExactPaths() {
        val shared = presentation(id = "shared", name = "Shared Workspace")
        val read = duplicateWorkspaceRead(shared)

        val results =
            createCanonicalV2SearchResults(
                filterState =
                    FilterState(
                        flatList = listOf(shared),
                        query = "Shared",
                        searchActive = true,
                        mode = PlanningMode.All,
                        settings = PlanningSettingsState(),
                        isReady = true,
                    ),
                read = read,
            )

        assertEquals(
            listOf("workspace-placement-a", "workspace-placement-b"),
            results.map { it.placementId },
        )
        assertEquals(
            listOf(
                listOf("No group", "Beacon A", "Shared Workspace"),
                listOf("No group", "Beacon B", "Shared Workspace"),
            ),
            results.map { it.parentPath },
        )
    }

    @Test
    fun exactBeaconBreadcrumbSelectsDuplicateOccurrenceAndPreservesPlacementIds() {
        val items =
            listOf(
                OrientationHierarchyItem(
                    node = OrientationHierarchyNode.Group("group-a", "Group A", 1),
                    level = 0,
                ),
                OrientationHierarchyItem(
                    node =
                        beacon(
                            id = "same-beacon",
                            title = "Same Beacon",
                            placementId = "beacon-placement-a",
                        ),
                    level = 1,
                ),
                OrientationHierarchyItem(
                    node = OrientationHierarchyNode.Group("group-b", "Group B", 1),
                    level = 0,
                ),
                OrientationHierarchyItem(
                    node =
                        beacon(
                            id = "same-beacon",
                            title = "Same Beacon",
                            placementId = "beacon-placement-b",
                        ),
                    level = 1,
                ),
            )

        val breadcrumbs =
            buildOrientationBreadcrumbs(
                items = items,
                nodeId = "same-beacon",
                placementId = "beacon-placement-b",
            )

        assertEquals(listOf("group-b", "same-beacon"), breadcrumbs.map { it.id })
        assertEquals(listOf(null, "beacon-placement-b"), breadcrumbs.map { it.placementId })
    }

    @Test
    fun exactOrientationLookupRejectsPlacementBelongingToDifferentTarget() {
        val items =
            listOf(
                OrientationHierarchyItem(
                    node =
                        beacon(
                            id = "beacon-a",
                            title = "Beacon A",
                            placementId = "placement-a",
                        ),
                    level = 0,
                ),
                OrientationHierarchyItem(
                    node =
                        beacon(
                            id = "beacon-b",
                            title = "Beacon B",
                            placementId = "placement-b",
                        ),
                    level = 0,
                ),
            )

        assertEquals(
            "beacon-b",
            findOrientationHierarchyItem(
                items = items,
                nodeId = "beacon-b",
                placementId = "placement-b",
            )?.node?.id,
        )
        assertEquals(
            null,
            findOrientationHierarchyItem(
                items = items,
                nodeId = "beacon-a",
                placementId = "placement-b",
            ),
        )
    }

    private fun duplicateWorkspaceRead(
        shared: HierarchyContextPresentationNode,
    ): CanonicalV2ProductionHierarchyRead =
        CanonicalV2ProductionHierarchyReadAdapter().read(
            placements =
                listOf(
                    placement(
                        id = "beacon-placement-a",
                        target = subjectTarget("beacon-a"),
                        order = 0,
                    ),
                    placement(
                        id = "workspace-placement-a",
                        target = workspaceTarget(shared.id),
                        parentId = "beacon-placement-a",
                        kind = PlacementKind.LINK,
                    ),
                    placement(
                        id = "beacon-placement-b",
                        target = subjectTarget("beacon-b"),
                        order = 1,
                    ),
                    placement(
                        id = "workspace-placement-b",
                        target = workspaceTarget(shared.id),
                        parentId = "beacon-placement-b",
                        kind = PlacementKind.LINK,
                    ),
                ),
            admittedWorkspacePresentations =
                listOf(shared.toCanonicalV2WorkspacePresentation()),
            managedSubjects =
                listOf(
                    subject("beacon-a", "Beacon A"),
                    subject("beacon-b", "Beacon B"),
                ),
            syntheticScopes =
                listOf(
                    CanonicalV2SyntheticScopeInput(
                        kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                        id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                        title = "No group",
                        order = 0,
                        rootPlacementIds =
                            listOf(
                                PlacementId("beacon-placement-a"),
                                PlacementId("beacon-placement-b"),
                            ),
                    ),
                ),
        )

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = HierarchyId.GENERAL,
        target = target,
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun subject(
        id: String,
        title: String,
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = "ORIENTATION",
        title = title,
        description = null,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun presentation(
        id: String,
        name: String,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = null,
        parentId = null,
        order = 0L,
        roleCode = null,
        tags = emptyList(),
    )

    private fun workspace(
        presentation: HierarchyContextPresentationNode,
        placementId: String,
    ) = OrientationHierarchyNode.WorkspaceNode(
        presentation = presentation,
        linkedBeaconIds = emptySet(),
        placementId = PlacementId(placementId),
    )

    private fun beacon(
        id: String,
        title: String,
        placementId: String,
    ) = OrientationHierarchyNode.Beacon(
        id = id,
        title = title,
        readinessStatus = MainBeaconReadinessStatus.READY,
        relatedContextCount = 0,
        placementId = PlacementId(placementId),
    )
}
