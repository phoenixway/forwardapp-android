package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceLinkedSemanticsViewModelTest {
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
    fun `standalone Context-backed and System Workspace use same read contract`() =
        runTest(dispatcher) {
            for (id in listOf("standalone", "context-backed", "system")) {
                val bindings = MutableStateFlow(
                    listOf(binding("orientation-1", WorkspaceBindingType.SUPPORTS)),
                )
                val catalog = MutableStateFlow(
                    listOf(orientation("orientation-1", "North star")),
                )
                val model = makeViewModel(id, bindings, catalog)

                backgroundScope.launch(dispatcher) {
                    model.uiState.collect()
                }
                advanceUntilIdle()

                val ready = model.uiState.value as WorkspaceLinkedSemanticsUiState.Ready
                assertThat(ready.links).hasSize(1)
                assertThat(ready.links.single().isNavigable).isTrue()
                assertThat(ready.links.single().ref)
                    .isEqualTo(CanonicalEntityRef.Orientation("orientation-1"))
            }
        }

    @Test
    fun `Workspace without bindings presents empty ready state`() =
        runTest(dispatcher) {
            val model = makeViewModel(
                "empty",
                MutableStateFlow(emptyList()),
                MutableStateFlow(emptyList()),
            )

            backgroundScope.launch(dispatcher) {
                model.uiState.collect()
            }
            advanceUntilIdle()

            val ready = model.uiState.value as WorkspaceLinkedSemanticsUiState.Ready
            assertThat(ready.links).isEmpty()
        }

    @Test
    fun `binding and catalog changes update projection without Context`() =
        runTest(dispatcher) {
            val bindings = MutableStateFlow<List<WorkspaceBindingEntity>>(emptyList())
            val catalog = MutableStateFlow(
                listOf(aspect("aspect-1", "Aspect")),
            )
            val model = makeViewModel("workspace-1", bindings, catalog)

            backgroundScope.launch(dispatcher) {
                model.uiState.collect()
            }
            advanceUntilIdle()

            assertThat(
                (model.uiState.value as WorkspaceLinkedSemanticsUiState.Ready).links,
            ).isEmpty()

            bindings.value = listOf(binding("aspect-1", WorkspaceBindingType.REALIZES))
            advanceUntilIdle()

            val linked = model.uiState.value as WorkspaceLinkedSemanticsUiState.Ready
            assertThat(linked.links.single().ref)
                .isEqualTo(CanonicalEntityRef.Aspect("aspect-1"))

            catalog.value = emptyList()
            advanceUntilIdle()

            val missing = model.uiState.value as WorkspaceLinkedSemanticsUiState.Ready
            assertThat(missing.links).hasSize(1)
            assertThat(missing.links.single().navigationTarget()).isNull()
        }

    private fun makeViewModel(
        workspaceId: String,
        bindings: MutableStateFlow<List<WorkspaceBindingEntity>>,
        catalog: MutableStateFlow<List<CanonicalEntitySummary>>,
    ): WorkspaceLinkedSemanticsViewModel {
        val bindingRepository = mockk<CanonicalWorkspaceBindingRepository>()
        val catalogRepository = mockk<CanonicalEntityCatalogRepository>()

        every {
            bindingRepository.observeBindingsForWorkspace(workspaceId)
        } returns bindings
        every { catalogRepository.observeCatalog() } returns catalog

        return WorkspaceLinkedSemanticsViewModel(
            SavedStateHandle(mapOf("workspaceId" to workspaceId)),
            bindingRepository,
            catalogRepository,
        )
    }

    private fun orientation(id: String, title: String) =
        CanonicalEntitySummary(CanonicalEntityRef.Orientation(id), title)

    private fun aspect(id: String, title: String) =
        CanonicalEntitySummary(CanonicalEntityRef.Aspect(id), title)

    private fun binding(
        subject: String,
        type: WorkspaceBindingType,
    ): WorkspaceBindingEntity =
        mockk<WorkspaceBindingEntity> {
            every { subjectId } returns subject
            every { bindingType } returns type.name
            every { isDeleted } returns false
        }
}
