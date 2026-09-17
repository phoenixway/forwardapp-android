package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RawContextHierarchyBackingTest {
    @Test
    fun rawBackingReusesExistingContextsByIdInsteadOfReconstructingThem() {
        val rawRoot = context("root", name = "Raw root", order = 11L)
        val rawChild = context("child", name = "Raw child", parentId = "root", order = 12L)

        val presentation =
            HierarchyPresentationData(
                allProjects =
                    listOf(
                        node("root", name = "Presentation root", order = 21L),
                        node(
                            "child",
                            name = "Presentation child",
                            parentId = "presentation-parent",
                            order = 22L,
                        ),
                    ),
                topLevelProjects =
                    listOf(
                        node("root", name = "Presentation root", order = 21L),
                    ),
                childMap =
                    mapOf(
                        "root" to
                            listOf(
                                node(
                                    "child",
                                    name = "Presentation child",
                                    parentId = "presentation-parent",
                                    order = 22L,
                                ),
                            ),
                    ),
            )

        val result =
            buildRawContextHierarchyBacking(
                presentationHierarchy = presentation,
                contexts = listOf(rawRoot, rawChild),
            )

        assertSame(rawRoot, result.rawContexts.first())
        assertSame(rawRoot, result.rawContextsById.getValue("root"))
        assertSame(rawChild, result.rawChildMap.getValue("root").single())
        assertEquals("Raw root", result.rawContextsById.getValue("root").name)
        assertEquals("Raw child", result.rawChildMap.getValue("root").single().name)
        assertEquals("root", result.rawChildMap.getValue("root").single().parentId)
        assertEquals(12L, result.rawChildMap.getValue("root").single().order)
    }

    @Test
    fun rawBackingFailsClosedForPresentationIdsWithoutExistingContext() {
        val rawRoot = context("root")

        val presentation =
            HierarchyPresentationData(
                allProjects =
                    listOf(
                        node("root"),
                        node("shell-free", parentId = "root"),
                    ),
                topLevelProjects = listOf(node("root")),
                childMap =
                    mapOf(
                        "root" to listOf(node("shell-free", parentId = "root")),
                    ),
            )

        val result =
            buildRawContextHierarchyBacking(
                presentationHierarchy = presentation,
                contexts = listOf(rawRoot),
            )

        assertEquals(listOf("root"), result.rawContexts.map { it.id })
        assertEquals(listOf("root"), result.rawContextsById.keys.toList())
        assertEquals(emptyMap<String, List<Context>>(), result.rawChildMap)
    }

    @Test
    fun rawBackingDropsChildrenWhenRawParentDoesNotExist() {
        val rawChild = context("child", parentId = "missing-parent")

        val presentation =
            HierarchyPresentationData(
                allProjects =
                    listOf(
                        node("missing-parent"),
                        node("child", parentId = "missing-parent"),
                    ),
                topLevelProjects = listOf(node("missing-parent")),
                childMap =
                    mapOf(
                        "missing-parent" to
                            listOf(
                                node("child", parentId = "missing-parent"),
                            ),
                    ),
            )

        val result =
            buildRawContextHierarchyBacking(
                presentationHierarchy = presentation,
                contexts = listOf(rawChild),
            )

        assertEquals(listOf("child"), result.rawContexts.map { it.id })
        assertEquals(emptyMap<String, List<Context>>(), result.rawChildMap)
    }

    @Test
    fun rawBackingPreservesPresentationAdmissionAndOrder() {
        val first = context("first")
        val second = context("second")
        val excluded = context("repository-only")
        val presentation =
            HierarchyPresentationData(
                allProjects = listOf(node("second"), node("shell-free"), node("first")),
                topLevelProjects = listOf(node("second"), node("shell-free"), node("first")),
            )

        val result =
            buildRawContextHierarchyBacking(
                presentationHierarchy = presentation,
                contexts = listOf(first, excluded, second),
            )

        assertEquals(listOf("second", "first"), result.rawContexts.map { it.id })
        assertSame(second, result.rawContextsById.getValue("second"))
        assertSame(first, result.rawContextsById.getValue("first"))
        assertEquals(false, "repository-only" in result.rawContextsById)
        assertEquals(false, "shell-free" in result.rawContextsById)
        assertEquals(emptyMap<String, List<Context>>(), result.rawChildMap)
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
        name: String = id,
        parentId: String? = null,
        order: Long = 0L,
    ) =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = emptyList(),
            order = order,
        )
}
