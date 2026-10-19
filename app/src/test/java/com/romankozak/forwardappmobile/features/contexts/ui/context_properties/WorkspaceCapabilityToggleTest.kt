package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.capability.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkspaceCapabilityToggleTest {
    private val workspace = mockk<CanonicalWorkspaceRepository>()
    private val backlog = mockk<CanonicalBacklogRepository>(relaxed = true)
    private val inbox = mockk<CanonicalInboxRepository>(relaxed = true)
    private val direction = mockk<CanonicalDirectionRepository>(relaxed = true)
    private val connections = mockk<CanonicalConnectionsRepository>(relaxed = true)
    private val sorting = mockk<CanonicalInboxSortingRepository>(relaxed = true)
    private val issues = mockk<CanonicalKeyProblemsRepository>(relaxed = true)
    private val dashboard = mockk<CanonicalDashboardCapabilityRepository>(relaxed = true)
    private val log = mockk<CanonicalExecutionLogRepository>(relaxed = true)
    private val commands = CanonicalOrdinaryCapabilitySettings(workspace, backlog, inbox, direction, connections,
        sorting, issues, dashboard, log)

    @Test fun `each toggle calls only its existing typed owner without applying a preset`() = runTest {
        coEvery { workspace.hasLiveWorkspace("w") } returns true
        workspaceFeatures.forEach { commands.setEnabled("w", CapabilityId(it.key), true) }
        coVerify(exactly = 1) { backlog.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { inbox.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { direction.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { connections.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { sorting.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { issues.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { dashboard.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { log.setEnabled("w", true, any()) }
        coVerify(exactly = 0) { workspace.updateRole(any(), any(), any()) }
    }

    @Test fun `missing Workspace and score or preset pseudo-capabilities fail closed`() = runTest {
        coEvery { workspace.hasLiveWorkspace("missing") } returns false
        coEvery { workspace.hasLiveWorkspace("w") } returns true
        assertThat(runCatching { commands.setEnabled("missing", CapabilityId("inbox"), true) }.isFailure).isTrue()
        listOf("score", "presets").forEach {
            assertThat(runCatching { commands.setEnabled("w", CapabilityId(it), true) }.isFailure).isTrue()
        }
        verify { backlog wasNot Called; inbox wasNot Called; direction wasNot Called; connections wasNot Called
            sorting wasNot Called; issues wasNot Called; dashboard wasNot Called; log wasNot Called }
    }
}
