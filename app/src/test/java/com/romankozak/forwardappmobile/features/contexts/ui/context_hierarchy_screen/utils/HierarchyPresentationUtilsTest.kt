package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildRawContextHierarchyBacking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HierarchyPresentationUtilsTest {
    @Test
    fun buildPresentationPathUsesPresentationTreeOrderAndNames() {
        val root = node("root", name = "Root", order = 2L)
        val child = node("child", name = "Child", parentId = "root", order = 1L)
        val leaf = node("leaf", name = "Leaf", parentId = "child", order = 3L)

        val hierarchy =
            HierarchyPresentationData(
                allProjects = listOf(root, child, leaf),
                topLevelProjects = listOf(root),
                childMap =
                    mapOf(
                        "root" to listOf(child),
                        "child" to listOf(leaf),
                    ),
            )

        assertEquals(
            listOf("Root", "Child", "Leaf"),
            buildPresentationPathToProject("leaf", hierarchy).map { it.name },
        )
    }

    @Test
    fun buildPresentationPathFailsClosedWhenTargetIsNotDisplayed() {
        val root = node("root")
        val detached = node("detached", parentId = "missing")

        val hierarchy =
            HierarchyPresentationData(
                allProjects = listOf(root, detached),
                topLevelProjects = listOf(root),
                childMap = emptyMap(),
            )

        assertEquals(
            emptyList<HierarchyContextPresentationNode>(),
            buildPresentationPathToProject("detached", hierarchy),
        )
    }

    @Test
    fun shellFreePresentationNodeHasNoLegacyContextBacking() {
        val parent = context("ordinary-parent")
        val shellFreeSystem =
            node(
                id = SystemContexts.INBOX.raw,
                name = "Canonical inbox",
                parentId = parent.id,
            )

        val presentation =
            HierarchyPresentationData(
                allProjects = listOf(parent.toHierarchyPresentationNode(), shellFreeSystem),
                topLevelProjects = listOf(parent.toHierarchyPresentationNode()),
                childMap = mapOf(parent.id to listOf(shellFreeSystem)),
            )

        val rawBacking =
            buildRawContextHierarchyBacking(
                presentationHierarchy = presentation,
                contexts = listOf(parent),
            )

        assertEquals(
            listOf(shellFreeSystem.id),
            presentation.childMap[parent.id].orEmpty().map { it.id },
        )
        assertEquals(parent.id, rawBacking.rawContextsById[parent.id]?.id)
        assertNull(rawBacking.rawContextsById[shellFreeSystem.id])
    }

    private fun node(
        id: String,
        name: String = id,
        parentId: String? = null,
        order: Long = 0L,
    ) =
        HierarchyContextPresentationNode(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            order = order,
            roleCode = null,
            tags = emptyList(),
        )

    private fun context(
        id: String,
        parentId: String? = null,
        order: Long = 0L,
    ) =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = emptyList(),
            order = order,
        )
}
