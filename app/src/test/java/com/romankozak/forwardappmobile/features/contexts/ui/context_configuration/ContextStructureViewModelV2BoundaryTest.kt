package com.romankozak.forwardappmobile.features.contexts.ui.context_configuration

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.domain.structure.PresetParentOccurrenceRequiredException
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureWithItems
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextStructureViewModelV2BoundaryTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val projectId = "parent"
    private val structures = mockk<ContextStructureRepository>(relaxed = true)
    private val presets = mockk<StructurePresetDao>(relaxed = true)
    private val service = mockk<StructurePresetService>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { structures.observeStructure(projectId) } returns
            flowOf(ContextStructureWithItems(ContextConfiguration.default(projectId), emptyList()))
        every { presets.getAll() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        ProjectStructureViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to projectId)),
            contextStructureRepository = structures,
            contextRepository = mockk<ContextRepository>(relaxed = true),
            structurePresetService = service,
            structurePresetDao = presets,
            canonicalDashboardCapabilityRepository = mockk<CanonicalDashboardCapabilityRepository>(relaxed = true),
            canonicalExecutionLogRepository = mockk<CanonicalExecutionLogRepository>(relaxed = true),
            systemCapabilityAccess = mockk<SystemContextCanonicalInboxDirectionAccess>(relaxed = true),
            systemRemainingCapabilityAccess = mockk<SystemContextCanonicalRemainingCapabilityLifecycleAccess>(relaxed = true),
            systemBacklogLifecycleAccess = mockk<SystemContextCanonicalBacklogLifecycleAccess>(relaxed = true),
        )

    @Test
    fun `manual preset failure displays message and clears loading without declaring preset applied`() =
        runTest(dispatcher) {
            coEvery { service.applyPresetToContext(projectId, "child_preset") } throws
                PresetParentOccurrenceRequiredException()
            val viewModel = viewModel()
            advanceUntilIdle()
            val initialPresetCode = viewModel.uiState.value.basePresetCode

            viewModel.applyPreset("child_preset")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.message)
            assertEquals(initialPresetCode, viewModel.uiState.value.basePresetCode)
            coVerify(exactly = 0) { structures.addOrUpdateItem(any(), any()) }
        }

    @Test
    fun `enabling SUBCONTEXT rejects before writing item or synchronizing structure`() =
        runTest(dispatcher) {
            val item = ContextStructureItem(
                id = "item",
                contextStructureId = "structure",
                entityType = "SUBCONTEXT",
                roleCode = "child",
                containerType = null,
                title = "Child",
                mandatory = false,
                isEnabled = false,
            )
            coEvery {
                service.requireTargetOnlyStructureSupported(projectId, true)
            } throws PresetParentOccurrenceRequiredException()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.toggleItem(item, enabled = true)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.message)
            coVerify(exactly = 0) { structures.setItemEnabled(any(), any()) }
            coVerify(exactly = 0) { service.applyContextStructure(any()) }
        }

    @Test
    fun `adding SUBCONTEXT rejects before ensuring structure or inserting item`() =
        runTest(dispatcher) {
            coEvery {
                service.requireTargetOnlyStructureSupported(projectId, true)
            } throws PresetParentOccurrenceRequiredException()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.addItem("SUBCONTEXT", "child", null, "Child", mandatory = false)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.message)
            coVerify(exactly = 0) { structures.addOrUpdateItem(any(), any()) }
            coVerify(exactly = 0) { service.applyContextStructure(any()) }
        }
}
