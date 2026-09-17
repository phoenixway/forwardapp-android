package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.backlog

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogConfigurationState
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogConfigurationAccess
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BacklogSettingsViewModelCanonicalTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `System setting reads canonical winner and routes write only through canonical access`() =
        runTest(dispatcher) {
            val id = SystemContexts.INBOX.raw
            val structures = mockk<ContextStructureRepository>(relaxed = true)
            val access = mockk<SystemContextCanonicalBacklogConfigurationAccess>()
            val canonical =
                SystemBacklogConfigurationState(
                    configuration = BacklogCapabilityConfigurationV2(true),
                    isEstablished = true,
                )
            every { access.handles(id) } returns true
            every { access.observeState(id) } returns flowOf(canonical)
            coEvery { access.setRemoveEntryAfterTagAutocopy(id, false, any()) } returns true
            val viewModel = BacklogSettingsViewModel(structures, access)

            viewModel.bind(id)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.removeAfterAutocopyEntriesWithTags)

            viewModel.onRemoveAfterAutocopyEntriesWithTagsChanged(false)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isSaving)
            coVerify(exactly = 1) { access.setRemoveEntryAfterTagAutocopy(id, false, any()) }
            coVerify(exactly = 0) { structures.updateStructure(any()) }
        }

    @Test
    fun `failed System setting reloads canonical winner and clears saving`() = runTest(dispatcher) {
        val id = SystemContexts.INBOX.raw
        val structures = mockk<ContextStructureRepository>(relaxed = true)
        val access = mockk<SystemContextCanonicalBacklogConfigurationAccess>()
        val canonical =
            SystemBacklogConfigurationState(
                configuration = BacklogCapabilityConfigurationV2(false),
                isEstablished = true,
            )
        every { access.handles(id) } returns true
        every { access.observeState(id) } returns flowOf(canonical)
        coEvery { access.getState(id) } returns canonical
        coEvery { access.setRemoveEntryAfterTagAutocopy(id, true, any()) } throws
            IllegalStateException("canonical write rejected")
        val viewModel = BacklogSettingsViewModel(structures, access)

        viewModel.bind(id)
        viewModel.onRemoveAfterAutocopyEntriesWithTagsChanged(true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.removeAfterAutocopyEntriesWithTags)
        assertFalse(viewModel.uiState.value.isSaving)
        coVerify(exactly = 0) { structures.updateStructure(any()) }
    }

    @Test
    fun `ordinary Context settings retain legacy structure behavior`() = runTest(dispatcher) {
        val id = "ordinary"
        val structures = mockk<ContextStructureRepository>()
        val access = mockk<SystemContextCanonicalBacklogConfigurationAccess>()
        val legacy = ContextConfiguration.default(id).copy(removeBacklogEntryAfterTagAutocopy = true)
        every { access.handles(id) } returns false
        every { structures.observeStructureOnly(id) } returns flowOf(legacy)
        coEvery { structures.ensureStructure(id) } returns legacy
        coEvery { structures.updateStructure(any()) } returns Unit
        val viewModel = BacklogSettingsViewModel(structures, access)

        viewModel.bind(id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.removeAfterAutocopyEntriesWithTags)
        viewModel.onRemoveAfterAutocopyEntriesWithTagsChanged(false)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            structures.updateStructure(match { it.removeBacklogEntryAfterTagAutocopy == false })
        }
        coVerify(exactly = 0) { access.setRemoveEntryAfterTagAutocopy(any(), any(), any()) }
    }
}
