package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ChooserProjection
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ReactiveHierarchyReadSource
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
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
            val canonicalWorkspaceRepository =
                mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val hierarchyReadSource =
                mockk<CanonicalV2ReactiveHierarchyReadSource>(relaxed = true)
            val chooserProjection =
                mockk<CanonicalV2ChooserProjection>(relaxed = true)

            every { hierarchyReadSource.observe() } returns flowOf()
            every { hierarchyReadSource.observeWorkspacePresentations() } returns flowOf()
            every { chooserProjection.project(any(), any()) } returns emptyList()

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
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    canonicalV2ReactiveHierarchyReadSource = hierarchyReadSource,
                    canonicalV2ChooserProjection = chooserProjection,
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
        }

    @Test
    fun `blank chooser creation authors nothing`() =
        runTest(dispatcher) {
            val canonicalWorkspaceRepository =
                mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val hierarchyReadSource =
                mockk<CanonicalV2ReactiveHierarchyReadSource>(relaxed = true)
            val chooserProjection =
                mockk<CanonicalV2ChooserProjection>(relaxed = true)

            every { hierarchyReadSource.observe() } returns flowOf()
            every { hierarchyReadSource.observeWorkspacePresentations() } returns flowOf()
            every { chooserProjection.project(any(), any()) } returns emptyList()

            val viewModel =
                FilterableListChooserViewModel(
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    canonicalV2ReactiveHierarchyReadSource = hierarchyReadSource,
                    canonicalV2ChooserProjection = chooserProjection,
                )

            assertNull(viewModel.addNewProject(parentId = null, name = "   "))

            coVerify(exactly = 0) {
                canonicalWorkspaceRepository.create(any(), any(), any(), any(), any())
            }
        }
}
