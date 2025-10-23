package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenStateUseCaseTest {

    @Test
    fun withHierarchyFallbackUsesAllProjectsWhenDefaultHierarchyEmpty() {
        val allProjects =
            listOf(
                project(id = "root"),
                project(id = "child", parentId = "root"),
            )
        val initialState =
            FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
            )

        val result = initialState.withHierarchyFallback(allProjects)

        assertEquals(allProjects, result.flatList)
    }

    @Test
    fun withHierarchyFallbackDoesNotOverrideWhenSearchIsActive() {
        val allProjects = listOf(project(id = "root"))
        val initialState =
            FilterState(
                flatList = emptyList(),
                query = "inbox",
                searchActive = true,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
            )

        val result = initialState.withHierarchyFallback(allProjects)

        assertTrue(result.flatList.isEmpty())
    }

    @Test
    fun withHierarchyFallbackKeepsNonDefaultModesIntact() {
        val allProjects = listOf(project(id = "root"))
        val initialState =
            FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.Today,
                settings = PlanningSettingsState(),
            )

        val result = initialState.withHierarchyFallback(allProjects)

        assertTrue(result.flatList.isEmpty())
    }

    private fun project(
        id: String,
        parentId: String? = null,
        order: Long = 0,
    ): Project =
        Project(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = null,
            order = order,
        )
}
