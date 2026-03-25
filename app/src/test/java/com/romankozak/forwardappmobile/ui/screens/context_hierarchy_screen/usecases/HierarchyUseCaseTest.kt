package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyUseCaseTest {
    private val useCase = HierarchyUseCase()

    @Test
    fun planningHierarchyIncludesTaggedBranchDescendants() {
        val root =
            project(
                id = "root",
                tags = listOf("focus"),
                order = 0,
            )
        val child =
            project(
                id = "child",
                parentId = "root",
                order = 0,
            )
        val grandchild =
            project(
                id = "grandchild",
                parentId = "child",
                order = 0,
            )
        val unrelated =
            project(
                id = "other",
                order = 1,
            )

        val filterState =
            FilterState(
                flatList = listOf(root, child, grandchild, unrelated),
                query = "",
                searchActive = false,
                mode = PlanningMode.Today,
                settings = PlanningSettingsState(dailyTag = "focus"),
                isReady = true,
            )

        val hierarchy =
            useCase.createProjectHierarchy(
                filterState = filterState,
                expandedDaily = null,
                expandedMedium = null,
                expandedLong = null,
            )

        assertEquals(listOf("root"), hierarchy.topLevelProjects.map { it.id })
        assertTrue(hierarchy.childMap["root"]?.any { it.id == "child" } == true)
        assertTrue(hierarchy.childMap["child"]?.any { it.id == "grandchild" } == true)
        assertTrue(hierarchy.childMap["root"]?.any { it.id == "other" } != true)
    }

    @Test
    fun regularHierarchyKeepsOrphanBranchNestedUnderFirstVisibleOrphan() {
        val orphanParentId = "missing-parent"
        val orphan =
            project(
                id = "orphan",
                parentId = orphanParentId,
                order = 0,
            )
        val orphanChild =
            project(
                id = "orphan-child",
                parentId = "orphan",
                order = 1,
            )

        val filterState =
            FilterState(
                flatList = listOf(orphan, orphanChild),
                query = "",
                searchActive = false,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
                isReady = true,
            )

        val hierarchy =
            useCase.createProjectHierarchy(
                filterState = filterState,
                expandedDaily = null,
                expandedMedium = null,
                expandedLong = null,
            )

        assertEquals(listOf("orphan"), hierarchy.topLevelProjects.map { it.id })
        assertEquals(listOf("orphan-child"), hierarchy.childMap["orphan"]?.map { it.id })
    }

    private fun project(
        id: String,
        parentId: String? = null,
        name: String = id,
        tags: List<String>? = null,
        order: Long = 0,
    ): Context =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = tags,
            order = order,
        )
}
