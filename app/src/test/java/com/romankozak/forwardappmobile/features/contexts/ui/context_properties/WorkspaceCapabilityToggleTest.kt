package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.capability.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkspaceCapabilityToggleTest {
    @Test fun `Semantics and prior labels resolve the same stable capability without touching Connections`() {
        listOf("Semantics", "Related entities", "Зв’язки").forEach {
            assertThat(featureLabelToCapabilityId(it)).isEqualTo(CapabilityId("relations"))
        }
        assertThat(featureLabelToCapabilityId("Connections")).isEqualTo(CapabilityId("connections"))
    }
    @Test fun `Semantics rename preserves capability identity and Connections name`() {
        assertThat(workspaceFeatures.single { it.key == "relations" })
            .isEqualTo(WorkspaceFeature("relations", "RELATIONS", "Semantics"))
        assertThat(workspaceFeatures.single { it.key == "connections" }.title).isEqualTo("Connections")
    }

    private val workspace = mockk<CanonicalWorkspaceRepository>()
    private val backlog = mockk<CanonicalBacklogRepository>(relaxed = true)
    private val inbox = mockk<CanonicalInboxRepository>(relaxed = true)
    private val direction = mockk<CanonicalDirectionRepository>(relaxed = true)
    private val connections = mockk<CanonicalConnectionsRepository>(relaxed = true)
    private val sorting = mockk<CanonicalInboxSortingRepository>(relaxed = true)
    private val issues = mockk<CanonicalKeyProblemsRepository>(relaxed = true)
    private val dashboard = mockk<CanonicalDashboardCapabilityRepository>(relaxed = true)
    private val log = mockk<CanonicalExecutionLogRepository>(relaxed = true)
    private val relations = mockk<CanonicalRelationsCapabilityRepository>(relaxed = true)
    private val commands = CanonicalOrdinaryCapabilitySettings(workspace, backlog, inbox, direction, connections,
        sorting, issues, dashboard, log, relations)

    @Test fun `each toggle calls only its existing typed owner without applying a preset`() = runTest {
        coEvery { workspace.hasLiveWorkspace("w") } returns true
        workspaceFeatures.forEach { commands.setEnabled("w", CapabilityId(it.key), true) }
        coVerify(exactly = 1) { backlog.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { inbox.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { direction.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { connections.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { sorting.setEnabled("w", true, any()) }
        coVerify(exactly = 1) { issues.setEnabled("w", true, any()) }
        coVerify(exactly = 0) { dashboard.setEnabled(any(), any(), any()) }
        coVerify(exactly = 0) { relations.setEnabled(any(), any(), any()) }
        coVerify(exactly = 1) { log.setEnabled("w", true, any()) }
        coVerify(exactly = 0) { workspace.updateRole(any(), any(), any()) }
    }

    @Test fun `required core views cannot be disabled and do not write optional metadata`() = runTest {
        coEvery { workspace.hasLiveWorkspace("w") } returns true
        commands.setEnabled("w", CapabilityId("dashboard"), false)
        commands.setEnabled("w", CapabilityId("relations"), false)
        verify { dashboard wasNot Called; relations wasNot Called }
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
