package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusedPresentationItemsTest {
    @Test
    fun activeHeaderOpensButDescendantFocusesWithinTheHierarchy() {
        assertEquals(
            ContextHierarchyScreenEvent.ContextClick("active"),
            focusedPresentationClickEvent(projectId = "active", isActiveHeader = true),
        )
        assertEquals(
            ContextHierarchyScreenEvent.FocusHierarchyProject("child"),
            focusedPresentationClickEvent(projectId = "child", isActiveHeader = false),
        )
    }

    @Test
    fun workspaceChildrenRemainInTheSingleFocusedProjectLikePath() {
        val parent = presentation("sys_parent", "Parent")
        val child = presentation("sys_child", "Child")
        val items =
            focusedPresentationItems(
                listOf(
                    OrientationHierarchyItem(
                        OrientationHierarchyNode.WorkspaceNode(parent, emptySet()),
                        level = 0,
                    ),
                    OrientationHierarchyItem(
                        OrientationHierarchyNode.WorkspaceNode(child, emptySet()),
                        level = 1,
                    ),
                ),
            )

        assertEquals(listOf("sys_parent", "sys_child"), items.map { it.project.id })
        assertEquals(listOf("Parent", "Child"), items.map { it.project.name })
    }

    private fun presentation(id: String, name: String) =
        HierarchyContextPresentationNode(
            id = id,
            name = name,
            description = null,
            parentId = null,
            order = 0L,
            roleCode = null,
            tags = emptyList(),
        )
}
