package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.withHierarchyFallback
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterStateExtensionsTest {

    @Test
    fun fallbackReusesAllProjectsWhenReadyAndAllMode() {
        val allProjects = listOf(project("root"), project("child", parentId = "root"))
        val state =
            FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
                isReady = true,
            )

        val result = state.withHierarchyFallback(allProjects)

        assertEquals(allProjects, result.flatList)
    }

    @Test
    fun fallbackKeepsStateWhenSearchActiveOrDifferentMode() {
        val allProjects = listOf(project("root"))
        val searchState =
            FilterState(
                flatList = emptyList(),
                query = "#tag",
                searchActive = true,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
                isReady = true,
            ).withHierarchyFallback(allProjects)
        val otherModeState =
            FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.Today,
                settings = PlanningSettingsState(),
                isReady = true,
            ).withHierarchyFallback(allProjects)

        assertTrue(searchState.flatList.isEmpty())
        assertTrue(otherModeState.flatList.isEmpty())
    }

    private fun project(id: String, parentId: String? = null): Project =
        Project(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = null,
            isExpanded = false,
            order = 0,
        )
}
