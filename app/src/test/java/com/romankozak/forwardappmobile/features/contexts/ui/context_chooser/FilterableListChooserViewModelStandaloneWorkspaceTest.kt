package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilterableListChooserViewModelStandaloneWorkspaceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `new chooser child uses canonical generated Workspace id and not Context`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository =
                mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val projector =
                mockk<SystemWorkspacePresentationContextProjector>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            every { projector.observePresentationUniverse(any()) } returns flowOf(emptyList())

            coEvery {
                canonicalWorkspaceRepository.create(
                    nameOverride = "Operations",
                    descriptionOverride = null,
                    parentWorkspaceId = "parent-workspace",
                    roleCode = null,
                    now = any(),
                )
            } returns "standalone-child"

            val viewModel =
                FilterableListChooserViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    systemWorkspacePresentationContextProjector = projector,
                )

            val id = viewModel.addNewProject(
                parentId = "parent-workspace",
                name = "  Operations  ",
            )

            assertEquals("standalone-child", id)

            coVerify(exactly = 1) {
                canonicalWorkspaceRepository.create(
                    nameOverride = "Operations",
                    descriptionOverride = null,
                    parentWorkspaceId = "parent-workspace",
                    roleCode = null,
                    now = any(),
                )
            }
            coVerify(exactly = 0) {
                contextRepository.createContextWithId(any(), any(), any())
            }
        }

    @Test
    fun `blank chooser creation authors nothing`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository =
                mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val projector =
                mockk<SystemWorkspacePresentationContextProjector>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            every { projector.observePresentationUniverse(any()) } returns flowOf(emptyList())

            val viewModel =
                FilterableListChooserViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    systemWorkspacePresentationContextProjector = projector,
                )

            assertNull(viewModel.addNewProject(parentId = null, name = "   "))

            coVerify(exactly = 0) {
                canonicalWorkspaceRepository.create(any(), any(), any(), any(), any())
            }
            coVerify(exactly = 0) {
                contextRepository.createContextWithId(any(), any(), any())
            }
        }
}
