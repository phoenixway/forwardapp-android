package com.romankozak.forwardappmobile.core.navigation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import org.junit.Test

class EntityLibraryNavigationContractTest {
    @Test
    fun `Library has a typed production destination`() {
        assertThat(NavTargetRouter.routeOf(NavTarget.EntityLibrary)).isEqualTo(NavigationRoutes.ENTITY_LIBRARY)
    }

    @Test
    fun `Library route is independent of Context hierarchy and Planning`() {
        val route = NavTargetRouter.routeOf(NavTarget.EntityLibrary)

        assertThat(route).doesNotContain("context")
        assertThat(route).doesNotContain("hierarchy")
        assertThat(route).doesNotContain("planning")
    }

    @Test
    fun `Workspace destination preserves typed canonical id`() {
        val route = NavTargetRouter.routeOf(NavTarget.Workspace("workspace/with spaces"))

        assertThat(route).isEqualTo("${NavigationRoutes.WORKSPACE}/workspace%2Fwith+spaces")
        assertThat(route).doesNotContain(NavigationRoutes.CONTEXT_DETAIL)
    }

    @Test
    fun `Workspace properties preserves canonical Workspace id`() {
        val route = NavTargetRouter.routeOf(NavTarget.WorkspaceProperties("workspace/with spaces"))

        assertThat(route).isEqualTo("${NavigationRoutes.WORKSPACE_PROPERTIES}/workspace%2Fwith+spaces")
        assertThat(route).doesNotContain(NavigationRoutes.PROJECT_SETTINGS)
        assertThat(NavigationRoutes.WORKSPACE_PROPERTIES_PATTERN).contains("{workspaceId}")
    }
}
