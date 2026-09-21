package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildDirectChildrenByOrientationNodeId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationHierarchyOccurrenceIdentityTest {
    @Test
    fun `direct children stay isolated for duplicate target occurrences`() {
        val first =
            workspace(
                targetId = "shared",
                placementId = "placement-a",
                level = 0,
            )
        val firstChild =
            workspace(
                targetId = "child-a",
                placementId = "child-a-placement",
                level = 1,
            )
        val second =
            workspace(
                targetId = "shared",
                placementId = "placement-b",
                level = 0,
            )
        val secondChild =
            workspace(
                targetId = "child-b",
                placementId = "child-b-placement",
                level = 1,
            )

        val children =
            buildDirectChildrenByOrientationNodeId(
                listOf(first, firstChild, second, secondChild),
            )

        assertEquals(
            listOf("child-a-placement"),
            children.getValue(first.node.structuralKey).map {
                requireNotNull(it.node.placementId).value
            },
        )
        assertEquals(
            listOf("child-b-placement"),
            children.getValue(second.node.structuralKey).map {
                requireNotNull(it.node.placementId).value
            },
        )
        assertEquals(2, children.keys.size)
    }

    @Test
    fun `legacy rows keep target id as structural key`() {
        val legacy =
            OrientationHierarchyNode.WorkspaceNode(
                presentation = presentation("legacy"),
                linkedBeaconIds = emptySet(),
            )

        assertEquals("legacy", legacy.structuralKey)
    }

    private fun workspace(
        targetId: String,
        placementId: String,
        level: Int,
    ): OrientationHierarchyItem =
        OrientationHierarchyItem(
            node =
                OrientationHierarchyNode.WorkspaceNode(
                    presentation = presentation(targetId),
                    linkedBeaconIds = emptySet(),
                    placementId = PlacementId(placementId),
                ),
            level = level,
        )

    private fun presentation(id: String): HierarchyContextPresentationNode =
        HierarchyContextPresentationNode(
            id = id,
            name = id,
            description = null,
            parentId = null,
            order = 0L,
            roleCode = null,
            tags = emptyList(),
        )
}
