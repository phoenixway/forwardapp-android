package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2HierarchyScreenPresentationAdapterTest {
    private val readAdapter = CanonicalV2ProductionHierarchyReadAdapter()
    private val screenAdapter = CanonicalV2HierarchyScreenPresentationAdapter()

    @Test
    fun `duplicate Workspace appearances retain exact PlacementId screen identity`() {
        val target =
            HierarchyTargetRef(
                type = HierarchyTargetType.WORKSPACE,
                id = "shared",
            )
        val presentation =
            HierarchyContextPresentationNode(
                id = "shared",
                name = "Shared",
                description = null,
                parentId = "legacy-parent-must-not-shape-v2",
                order = 999L,
                roleCode = null,
                tags = emptyList(),
            )
        val read =
            readAdapter.read(
                placements =
                    listOf(
                        placement("primary", target, PlacementKind.PRIMARY, order = 10L),
                        placement("link", target, PlacementKind.LINK, order = 20L),
                    ),
                admittedWorkspacePresentations = listOf(presentation),
                managedSubjects = emptyList(),
                presentationProvenance =
                    CanonicalV2HierarchyPresentationProvenance(
                        linkedAppearancePlacementIds = setOf(PlacementId("link")),
                    ),
            )

        val rows =
            screenAdapter.adapt(
                read = read,
                metadata =
                    CanonicalV2HierarchyScreenMetadata(
                        workspacePresentationsByTargetId = mapOf("shared" to presentation),
                        linkedBeaconIdsByWorkspaceTargetId =
                            mapOf("shared" to setOf("beacon-operational-owner")),
                    ),
            )

        assertEquals(3, rows.size)
        assertTrue(rows.first().node is OrientationHierarchyNode.NoBeacon)
        assertNull(rows.first().node.placementId)

        val workspaces =
            rows
                .drop(1)
                .map { it.node as OrientationHierarchyNode.WorkspaceNode }

        assertEquals(listOf("shared", "shared"), workspaces.map { it.id })
        assertEquals(
            listOf(PlacementId("primary"), PlacementId("link")),
            workspaces.map { it.placementId },
        )
        assertEquals(
            listOf(
                HierarchyOccurrenceRef(
                    placementId = PlacementId("primary"),
                    target = target,
                    parentPlacementId = null,
                    placementKind = PlacementKind.PRIMARY,
                    siblingOrder = 10L,
                ),
                HierarchyOccurrenceRef(
                    placementId = PlacementId("link"),
                    target = target,
                    parentPlacementId = null,
                    placementKind = PlacementKind.LINK,
                    siblingOrder = 20L,
                ),
            ),
            workspaces.map { it.occurrence },
        )
        assertEquals(2, workspaces.map { it.occurrence?.placementId }.distinct().size)
        assertEquals(2, workspaces.map { it.structuralKey }.distinct().size)
        assertFalse(workspaces[0].isLinkedAppearance)
        assertTrue(workspaces[1].isLinkedAppearance)
        assertEquals(
            setOf("beacon-operational-owner"),
            workspaces[0].linkedBeaconIds,
        )
        assertEquals(
            listOf(1, 1),
            rows.drop(1).map { it.level },
        )
    }

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        kind: PlacementKind,
        order: Long,
    ): HierarchyPlacement =
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target = target,
            parentPlacementId = null,
            placementKind = kind,
            siblingOrder = order,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
