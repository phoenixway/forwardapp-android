package com.romankozak.forwardappmobile.features.contexts.ui.context_configuration

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureWithItems
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextStructureViewModelEnableAdvancedRetirementTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `unrelated capability toggle preserves historical enableAdvanced`() = runTest(dispatcher) {
        val contextId = "ordinary"
        val configuration = ContextConfiguration.default(contextId).copy(enableAdvanced = true)
        val structures = mockk<ContextStructureRepository>(relaxed = true)
        every { structures.observeStructure(contextId) } returns
            flowOf(ContextStructureWithItems(configuration, emptyList()))
        coEvery { structures.ensureStructure(contextId) } returns configuration
        val presetDao = mockk<StructurePresetDao>(relaxed = true)
        every { presetDao.getAll() } returns flowOf(emptyList())
        val systemAccess = mockk<SystemContextCanonicalInboxDirectionAccess>(relaxed = true)
        every { systemAccess.handles(contextId) } returns false
        val remainingAccess = mockk<SystemContextCanonicalRemainingCapabilityLifecycleAccess>(relaxed = true)
        every { remainingAccess.handles(contextId) } returns false
        val viewModel =
            ProjectStructureViewModel(
                savedStateHandle = SavedStateHandle(mapOf("projectId" to contextId)),
                contextStructureRepository = structures,
                contextRepository = mockk<ContextRepository>(relaxed = true),
                structurePresetService = mockk<StructurePresetService>(relaxed = true),
                structurePresetDao = presetDao,
                canonicalDashboardCapabilityRepository =
                    mockk<CanonicalDashboardCapabilityRepository>(relaxed = true),
                canonicalExecutionLogRepository =
                    mockk<CanonicalExecutionLogRepository>(relaxed = true),
                systemCapabilityAccess = systemAccess,
                systemRemainingCapabilityAccess = remainingAccess,
                systemBacklogLifecycleAccess = mockk<SystemContextCanonicalBacklogLifecycleAccess>(relaxed = true),
            )
        advanceUntilIdle()

        viewModel.onToggleFeatureFlag("Inbox", enabled = false)
        advanceUntilIdle()

        coVerify {
            structures.updateStructure(match { it.enableAdvanced == true })
        }
    }

    @Test
    fun `System Backlog reads canonical lifecycle and toggle uses canonical command`() =
        runTest(dispatcher) {
            val contextId = SystemContexts.INBOX.raw
            val configuration = ContextConfiguration.default(contextId).copy(enableBacklog = true)
            val structures = mockk<ContextStructureRepository>(relaxed = true)
            every { structures.observeStructure(contextId) } returns
                flowOf(ContextStructureWithItems(configuration, emptyList()))
            coEvery { structures.ensureStructure(contextId) } returns configuration
            coEvery { structures.getStructureByContext(contextId) } returns configuration
            val presetDao = mockk<StructurePresetDao>(relaxed = true)
            every { presetDao.getAll() } returns flowOf(emptyList())
            val systemAccess = mockk<SystemContextCanonicalInboxDirectionAccess>(relaxed = true)
            every { systemAccess.handles(contextId) } returns true
            val remainingAccess =
                mockk<SystemContextCanonicalRemainingCapabilityLifecycleAccess>(relaxed = true)
            every { remainingAccess.handles(contextId) } returns true
            val backlogAccess = mockk<SystemContextCanonicalBacklogLifecycleAccess>(relaxed = true)
            every { backlogAccess.handles(contextId) } returns true
            coEvery { backlogAccess.getState(contextId) } returns
                SystemBacklogLifecycleState(
                    backlog =
                        BacklogCapabilityState(
                            lifecycleState = WorkspaceCapabilityState.DISABLED,
                            isDeleted = false,
                            configuration = BacklogCapabilityConfigurationV2(false),
                        ),
                    isEstablished = true,
                )
            val viewModel =
                ProjectStructureViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("projectId" to contextId)),
                    contextStructureRepository = structures,
                    contextRepository = mockk<ContextRepository>(relaxed = true),
                    structurePresetService = mockk<StructurePresetService>(relaxed = true),
                    structurePresetDao = presetDao,
                    canonicalDashboardCapabilityRepository =
                        mockk<CanonicalDashboardCapabilityRepository>(relaxed = true),
                    canonicalExecutionLogRepository =
                        mockk<CanonicalExecutionLogRepository>(relaxed = true),
                    systemCapabilityAccess = systemAccess,
                    systemRemainingCapabilityAccess = remainingAccess,
                    systemBacklogLifecycleAccess = backlogAccess,
                )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.featureFlags.getValue("Backlog"))

            viewModel.onToggleFeatureFlag("Backlog", enabled = true)
            advanceUntilIdle()

            coVerify(exactly = 1) { backlogAccess.setEnabled(contextId, true, any()) }
        }

    @Test
    fun `preset application delegates lifecycle ownership without Dashboard or Log commands`() =
        runTest(dispatcher) {
            val contextId = SystemContexts.INBOX.raw
            val configuration = ContextConfiguration.default(contextId)
            val structures = mockk<ContextStructureRepository>(relaxed = true)
            every { structures.observeStructure(contextId) } returns
                flowOf(ContextStructureWithItems(configuration, emptyList()))
            val presetDao = mockk<StructurePresetDao>(relaxed = true)
            every { presetDao.getAll() } returns flowOf(emptyList())
            val presetService = mockk<StructurePresetService>(relaxed = true)
            val dashboard = mockk<CanonicalDashboardCapabilityRepository>(relaxed = true)
            val executionLog = mockk<CanonicalExecutionLogRepository>(relaxed = true)
            val systemAccess = mockk<SystemContextCanonicalInboxDirectionAccess>(relaxed = true)
            every { systemAccess.handles(contextId) } returns true
            val remainingAccess =
                mockk<SystemContextCanonicalRemainingCapabilityLifecycleAccess>(relaxed = true)
            every { remainingAccess.handles(contextId) } returns true
            val viewModel =
                ProjectStructureViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("projectId" to contextId)),
                    contextStructureRepository = structures,
                    contextRepository = mockk<ContextRepository>(relaxed = true),
                    structurePresetService = presetService,
                    structurePresetDao = presetDao,
                    canonicalDashboardCapabilityRepository = dashboard,
                    canonicalExecutionLogRepository = executionLog,
                    systemCapabilityAccess = systemAccess,
                    systemRemainingCapabilityAccess = remainingAccess,
                    systemBacklogLifecycleAccess =
                        mockk<SystemContextCanonicalBacklogLifecycleAccess>(relaxed = true),
                )
            advanceUntilIdle()

            viewModel.applyPreset("crisis_case")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                presetService.applyPresetToContext(contextId, "crisis_case")
            }
            coVerify(exactly = 0) { dashboard.setEnabled(any(), any(), any()) }
            coVerify(exactly = 0) { executionLog.setEnabled(any(), any(), any()) }
        }
}
