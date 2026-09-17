package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.direction

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.capability.DirectionCapabilityState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
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
class DirectionSettingsViewModelFailureTest {
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
    fun `failed System Direction configuration command restores canonical value and clears saving`() =
        runTest(dispatcher) {
            val contextId = SystemContexts.INBOX.raw
            val structureRepository = mockk<ContextStructureRepository>(relaxed = true)
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val access = mockk<SystemContextCanonicalInboxDirectionAccess>()
            val canonical =
                SystemInboxDirectionState(
                    inbox = null,
                    direction =
                        DirectionCapabilityState(
                            lifecycleState = WorkspaceCapabilityState.ACTIVE,
                            isDeleted = false,
                            configuration = DirectionCapabilityConfigurationV1(false),
                        ),
                )
            every { access.handles(contextId) } returns true
            every { access.observeState(contextId) } returns flowOf(canonical)
            coEvery { access.getState(contextId) } returns canonical
            coEvery { access.updateDirectionConfiguration(contextId, any(), any()) } throws
                IllegalStateException("canonical write rejected")
            val viewModel =
                DirectionSettingsViewModel(
                    contextStructureRepository = structureRepository,
                    contextRepository = contextRepository,
                    systemCapabilityAccess = access,
                )

            viewModel.bind(contextId)
            advanceUntilIdle()
            viewModel.onAutoAddChildContextToDirectionFrontChanged(true)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.autoAddChildContextToDirectionFront)
            assertFalse(viewModel.uiState.value.isSaving)
            assertNotNull(viewModel.uiState.value.errorMessage)
            coVerify(exactly = 0) { structureRepository.updateStructure(any()) }
            coVerify(exactly = 0) {
                contextRepository.ensureDirectionFrontLinksForExistingChildren(any())
            }
        }
}
