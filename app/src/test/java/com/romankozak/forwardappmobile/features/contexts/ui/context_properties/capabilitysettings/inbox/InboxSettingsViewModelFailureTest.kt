package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.inbox

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.capability.InboxCapabilityState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxSettingsViewModelFailureTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed System Inbox configuration command restores canonical value and clears saving`() =
        runTest(dispatcher) {
            val contextId = SystemContexts.INBOX.raw
            val structureRepository = mockk<ContextStructureRepository>(relaxed = true)
            val access = mockk<SystemContextCanonicalInboxDirectionAccess>()
            val canonical =
                SystemInboxDirectionState(
                    inbox =
                        InboxCapabilityState(
                            lifecycleState = WorkspaceCapabilityState.ACTIVE,
                            isDeleted = false,
                            configuration =
                                InboxCapabilityConfigurationV1(
                                    ownerVisibility = InboxOwnerVisibility.KEEP_VISIBLE,
                                ),
                        ),
                    direction = null,
                )
            every { access.handles(contextId) } returns true
            every { access.observeState(contextId) } returns flowOf(canonical)
            coEvery { access.getState(contextId) } returns canonical
            coEvery { access.updateInboxConfiguration(contextId, any(), any()) } throws
                IllegalStateException("canonical write rejected")
            val viewModel = InboxSettingsViewModel(structureRepository, access)

            viewModel.bind(contextId)
            advanceUntilIdle()
            viewModel.onRemoveAfterAutocopyEntriesWithTagsChanged(true)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.removeAfterAutocopyEntriesWithTags)
            assertFalse(viewModel.uiState.value.isSaving)
            assertNotNull(viewModel.uiState.value.errorMessage)
            coVerify(exactly = 0) { structureRepository.updateStructure(any()) }
        }
}
