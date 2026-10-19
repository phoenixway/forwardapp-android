package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkspaceOperationalRouteTest {
    @Test fun `canonical identity wins over restored legacy identity and compatibility metadata`() {
        val saved = SavedStateHandle(mapOf(
            "workspaceId" to "workspace:/exact", "listId" to "stale-context", "sourceContextId" to "legacy",
        ))
        val route = WorkspaceOperationalRoute(saved)
        assertThat(route.isCanonicalWorkspace).isTrue()
        assertThat(route.id.value).isEqualTo("workspace:/exact")
        saved["listId"] = "another-context"
        assertThat(route.id.value).isEqualTo("workspace:/exact")
        assertThat(WorkspaceOperationalRoute(saved).id.value).isEqualTo("workspace:/exact")
    }

    @Test fun `invalid canonical argument never falls back to Context`() {
        listOf("", null).forEach { invalid ->
            val route = WorkspaceOperationalRoute(SavedStateHandle(mapOf("workspaceId" to invalid, "listId" to "context")))
            assertThat(route.isCanonicalWorkspace).isTrue()
            assertThat(route.id.value).isEmpty()
        }
    }

    @Test fun `legacy operational entry retains its existing reactive identity`() {
        val saved = SavedStateHandle(mapOf("listId" to "legacy"))
        val route = WorkspaceOperationalRoute(saved)
        assertThat(route.isCanonicalWorkspace).isFalse()
        assertThat(route.id.value).isEqualTo("legacy")
        saved["listId"] = "legacy-next"
        assertThat(route.id.value).isEqualTo("legacy-next")
    }

    @Test fun `both Workspace properties entry points cannot use compatibility Context identity`() {
        assertThat(workspacePropertiesDestination("canonical", "compatibility"))
            .isEqualTo(NavTarget.WorkspaceProperties("canonical"))
        assertThat(workspacePropertiesDestination(null, "legacy"))
            .isEqualTo(NavTarget.ProjectSettings(projectId = "legacy"))
    }

    @Test fun `opening canonical operational surface never invokes Direction auto mutation`() = runTest {
        val repository = mockk<ContextRepository>()
        val route = WorkspaceOperationalRoute(SavedStateHandle(mapOf("workspaceId" to "workspace")))
        observeLegacyDirectionLinksOnOpen(route, backgroundScope, UnconfinedTestDispatcher(testScheduler), repository)
        runCurrent()
        coVerify(exactly = 0) { repository.ensureDirectionFrontLinksForExistingChildren(any()) }
    }

    @Test fun `legacy auto-linking policy remains available only on explicit legacy entry`() = runTest {
        val repository = mockk<ContextRepository>()
        coEvery { repository.ensureDirectionFrontLinksForExistingChildren("legacy") } returns 0
        val route = WorkspaceOperationalRoute(SavedStateHandle(mapOf("listId" to "legacy")))
        observeLegacyDirectionLinksOnOpen(route, backgroundScope, UnconfinedTestDispatcher(testScheduler), repository)
        runCurrent()
        coVerify(exactly = 1) { repository.ensureDirectionFrontLinksForExistingChildren("legacy") }
    }

    @Test fun `adapted operational product copy identifies Workspace not Context`() {
        assertThat(WorkspaceSurfaceCopy.TITLE).isEqualTo("Робочий простір")
        assertThat(WorkspaceSurfaceCopy.PROPERTIES).isEqualTo("Властивості робочого простору")
        listOf(WorkspaceSurfaceCopy.FOCUS_ADD, WorkspaceSurfaceCopy.FOCUS_REMOVE,
            WorkspaceSurfaceCopy.START_TRACKING, WorkspaceSurfaceCopy.EMPTY_DOCUMENTS,
            WorkspaceSurfaceCopy.ADVANCED_MODE, WorkspaceSurfaceCopy.SOURCE_LINK,
            WorkspaceSurfaceCopy.PASTE_ATTACHMENTS, WorkspaceSurfaceCopy.CHOOSE_VIEW,
            WorkspaceSurfaceCopy.EMPTY_CONNECTIONS, WorkspaceSurfaceCopy.DASHBOARD, WorkspaceSurfaceCopy.DELETE).forEach {
            assertThat(it.lowercase()).doesNotContain("контекст")
            assertThat(it).contains("робоч")
        }
    }
}
