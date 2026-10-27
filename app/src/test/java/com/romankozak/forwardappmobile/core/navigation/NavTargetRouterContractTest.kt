package com.romankozak.forwardappmobile.core.navigation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import org.junit.Test

class NavTargetRouterContractTest {
    @Test fun `Big Picture exact reveal carries encoded placement and Workspace identities`() {
        val target = NavTarget.ContextHierarchy(projectIdToReveal = "workspace/1", placementIdToReveal = "link/2")
        assertThat(NavTargetRouter.routeOf(target)).isEqualTo(NavigationRoutes.goalLists("workspace/1", "link/2"))
        assertThat(NavTargetRouter.routeOf(target)).contains("placementIdToReveal=link%2F2")
        assertThat(NavTargetRouter.routeOf(target)).contains("projectIdToReveal=workspace%2F1")
    }

    @Test fun `existing root and Workspace-only Big Picture routes remain valid`() {
        assertThat(NavTargetRouter.routeOf(NavTarget.ContextHierarchy())).isEqualTo(NavigationRoutes.GOAL_LISTS)
        assertThat(NavTargetRouter.routeOf(NavTarget.ContextHierarchy("w"))).doesNotContain("placementIdToReveal")
    }

    @Test
    fun `day management target uses shared day_management route contract`() {
        val route = NavTargetRouter.routeOf(NavTarget.DayManagement(date = 123L, startTab = "PLAN"))

        assertThat(route).isEqualTo(NavigationRoutes.dayManagement(date = 123L, startTab = "PLAN"))
        assertThat(route).startsWith("${NavigationRoutes.DAY_MANAGEMENT}/")
    }

    @Test
    fun `import export target uses shared selective import query contract`() {
        val route = NavTargetRouter.routeOf(NavTarget.ImportExport(uri = "content://backup/path"))

        assertThat(route).startsWith("${NavigationRoutes.SELECTIVE_IMPORT}?${NavigationRoutes.ARG_FILE_URI}=")
        assertThat(route).contains("content%3A%2F%2Fbackup%2Fpath")
    }

    @Test
    fun `script editor target uses projectId query key`() {
        val route = NavTargetRouter.routeOf(NavTarget.ScriptEditor(contextId = "ctx-1", scriptId = "sc-7"))

        assertThat(route).isEqualTo(NavigationRoutes.scriptEditor(projectId = "ctx-1", scriptId = "sc-7"))
        assertThat(route).contains("projectId=ctx-1")
    }
}
