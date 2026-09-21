package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalV2SearchOccurrenceIdentityTest {
    @Test
    fun duplicateWorkspaceAppearancesRemainDistinctSearchResultsWithExactPaths() {
        val shared = presentation(id = "shared", name = "Shared Workspace")
        val orientation =
            listOf(
                OrientationHierarchyItem(
                    node =
                        beacon(
                            id = "beacon-a",
                            title = "Beacon A",
                            placementId = "beacon-placement-a",
                        ),
                    level = 0,
                ),
                OrientationHierarchyItem(
                    node = workspace(shared, placementId = "workspace-placement-a"),
                    level = 1,
                ),
                OrientationHierarchyItem(
                    node =
                        beacon(
                            id = "beacon-b",
                            title = "Beacon B",
                            placementId = "beacon-placement-b",
                        ),
                    level = 0,
                ),
                OrientationHierarchyItem(
                    node = workspace(shared, placementId = "workspace-placement-b"),
                    level = 1,
                ),
            )

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
                orientationHierarchy = orientation,
            )

        assertEquals(
            listOf("workspace-placement-a", "workspace-placement-b"),
            results.map { it.placementId },
        )
        assertEquals(
            listOf(
                listOf("Beacon A", "Shared Workspace"),
                listOf("Beacon B", "Shared Workspace"),
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
