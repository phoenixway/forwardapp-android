package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HierarchyPresentationTreeBuilderTest {
    private val builder = HierarchyPresentationTreeBuilder()

    @Test
    fun contextMapperCarriesOnlyHierarchyPresentationFields() {
        val node =
            context(
                id = "project",
                name = "Project",
                description = "Description",
                parentId = "parent",
                order = 4L,
                roleCode = "role",
                tags = listOf("focus"),
            ).toHierarchyPresentationNode()

        assertEquals(
            HierarchyContextPresentationNode(
                id = "project",
                name = "Project",
                description = "Description",
                parentId = "parent",
                order = 4L,
                roleCode = "role",
                tags = listOf("focus"),
            ),
            node,
        )
        val persistenceOnlyFields =
            setOf(
                "isExpanded",
                "isAttachmentsExpanded",
                "showCheckboxes",
                "isCompleted",
                "isDeleted",
                "createdAt",
                "updatedAt",
                "syncedAt",
                "version",
            )
        assertFalse(
            HierarchyContextPresentationNode::class.java.declaredFields
                .map { it.name }
                .any { it in persistenceOnlyFields },
        )
    }

    @Test
    fun treeMatchesExistingRegularHierarchyForOrdinaryContexts() {
        val contexts =
            listOf(
                context("root", order = 2L),
                context("first", parentId = "root", order = 1L),
                context("second", parentId = "root", order = 2L),
                context("orphan", parentId = "missing", order = 1L),
                context("orphan-child", parentId = "orphan", order = 0L),
            )

        val hierarchy = builder.build(contexts.map(Context::toHierarchyPresentationNode))

        assertEquals(contexts.map { it.id }, hierarchy.allProjects.map { it.id })
        assertEquals(listOf("orphan", "root"), hierarchy.topLevelProjects.map { it.id })
        assertEquals(listOf("first", "second"), hierarchy.childMap["root"]?.map { it.id })
        assertEquals(listOf("orphan-child"), hierarchy.childMap["orphan"]?.map { it.id })
    }

    @Test
    fun preservesMultiLevelOrderingAndTreatsNonReservedSysIdAsOrdinaryNode() {
        val hierarchy =
            builder.build(
                listOf(
                    node("sys_custom", order = 1L),
                    node("child-late", parentId = "sys_custom", order = 2L),
                    node("child-early", parentId = "sys_custom", order = 1L),
                    node("grandchild", parentId = "child-early", order = 0L),
                ),
            )

        assertEquals(listOf("sys_custom"), hierarchy.topLevelProjects.map { it.id })
        assertEquals(listOf("child-early", "child-late"), hierarchy.childMap["sys_custom"]?.map { it.id })
        assertEquals(listOf("grandchild"), hierarchy.childMap["child-early"]?.map { it.id })
    }

    @Test
    fun cyclicParentsMatchExistingFailClosedTopLevelBehavior() {
        val hierarchy =
            builder.build(
                listOf(
                    node("first", parentId = "second", order = 1L),
                    node("second", parentId = "first", order = 2L),
                ),
            )

        assertEquals(listOf("first", "second"), hierarchy.topLevelProjects.map { it.id })
        assertEquals(emptyMap<String, List<HierarchyContextPresentationNode>>(), hierarchy.childMap)
    }

    @Test
    fun presentationTreeHasNoPersistenceSideEffects() {
        val source = context("project", tags = null)
        val node = source.toHierarchyPresentationNode()

        builder.build(listOf(node))

        assertEquals(emptyList<String>(), node.tags)
        assertFalse(HierarchyContextPresentationNode::class.java.declaredFields.any { it.name == "isDeleted" })
    }

    private fun node(
        id: String,
        parentId: String? = null,
        order: Long = 0L,
    ) =
        HierarchyContextPresentationNode(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            order = order,
            roleCode = null,
            tags = emptyList(),
        )

    private fun context(
        id: String,
        name: String = id,
        description: String? = null,
        parentId: String? = null,
        order: Long = 0L,
        roleCode: String? = null,
        tags: List<String>? = emptyList(),
    ) =
        Context(
            id = id,
            name = name,
            description = description,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = tags,
            order = order,
            roleCode = roleCode,
        )
}
