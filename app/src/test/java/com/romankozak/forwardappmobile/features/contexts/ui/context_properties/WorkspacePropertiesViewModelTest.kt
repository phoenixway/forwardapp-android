package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspacePropertiesSnapshot
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspacePropertiesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `standalone Workspace loads canonically and does not require Context`() = runTest(dispatcher) {
        val fixture = fixture(snapshot(sourceContextId = null))

        val model = fixture.viewModel.ready()

        assertThat(model.workspaceId).isEqualTo("workspace-1")
        assertThat(model.name).isEqualTo("Canonical Workspace")
        assertThat(model.sourceContextId).isNull()
        assertThat(model.bindings).isEmpty()
    }

    @Test
    fun `context backed and System Workspaces retain canonical properties identity`() = runTest(dispatcher) {
        val contextBacked = fixture(snapshot(sourceContextId = "legacy-context")).viewModel.ready()
        val system = fixture(snapshot(id = "inbox", isSystem = true)).viewModel.ready()

        assertThat(contextBacked.sourceContextId).isEqualTo("legacy-context")
        assertThat(contextBacked.name).isEqualTo("Canonical Workspace")
        assertThat(system.isSystem).isTrue()
    }

    @Test
    fun `save writes canonical Workspace presentation only`() = runTest(dispatcher) {
        val fixture = fixture(snapshot())
        fixture.viewModel.onNameChange("Renamed")
        fixture.viewModel.onDescriptionChange("Updated")

        fixture.viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            fixture.workspaceRepository.updatePropertiesPresentation(
                id = "workspace-1",
                name = "Renamed",
                description = "Updated",
                now = any(),
            )
        }
    }

    @Test
    fun `workspace side binding preserves typed subject and explicit type`() = runTest(dispatcher) {
        val fixture = fixture(snapshot())
        fixture.viewModel.acceptPickerResult(setOf(CanonicalEntityRef.Aspect("aspect-1")))

        fixture.viewModel.addBinding(WorkspaceBindingType.MONITORS)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            fixture.bindingRepository.bind("aspect-1", "workspace-1", WorkspaceBindingType.MONITORS, now = any())
        }
        assertThat(fixture.viewModel.semanticEntityPickerRequest().allowedClasses)
            .containsExactly(CanonicalEntityClass.ORIENTATION, CanonicalEntityClass.ASPECT)
        assertThat(fixture.viewModel.semanticEntityPickerRequest().excludedRefs).isEmpty()
    }

    @Test
    fun `binding summary resolves all labels in one lightweight catalog flow`() = runTest(dispatcher) {
        val bindingFlow = MutableStateFlow(listOf(binding()))
        val fixture = fixture(snapshot(), bindingFlow)

        val binding = fixture.viewModel.ready().bindings.single()

        assertThat(binding.title).isEqualTo("Health")
        assertThat(binding.entityClass).isEqualTo(CanonicalEntityClass.ASPECT)
        assertThat(binding.bindingType).isEqualTo(WorkspaceBindingType.SUPPORTS)
    }

    @Test
    fun `remove delegates only to canonical WorkspaceBinding authority`() = runTest(dispatcher) {
        val fixture = fixture(snapshot(), MutableStateFlow(listOf(binding())))

        fixture.viewModel.removeBinding("binding-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { fixture.bindingRepository.unbind("binding-1", now = any()) }
    }

    @Test
    fun `missing or tombstoned Workspace fails closed`() = runTest(dispatcher) {
        val fixture = fixture(null)

        assertThat(fixture.viewModel.uiState.value)
            .isInstanceOf(WorkspacePropertiesUiState.Unavailable::class.java)
    }

    @Test
    fun `invalid binding target fails closed and observation can recover`() = runTest(dispatcher) {
        val bindings = MutableStateFlow(listOf(binding().copy(subjectId = "missing-subject")))
        val fixture = fixture(snapshot(), bindings)

        assertThat(fixture.viewModel.uiState.value)
            .isInstanceOf(WorkspacePropertiesUiState.Unavailable::class.java)

        bindings.value = listOf(binding())
        advanceUntilIdle()

        assertThat(fixture.viewModel.ready().bindings.single().subjectId)
            .isEqualTo("aspect-1")
    }

    @Test
    fun `legacy edit route resolves canonical Workspace without a Context-settings hop`() = runTest(dispatcher) {
        val f = fixture(snapshot(), saved = SavedStateHandle(mapOf("projectId" to "workspace-1")))
        assertThat(f.viewModel.ready().workspaceId).isEqualTo("workspace-1")
        f.viewModel.onNameChange("Canonical edit")
        f.viewModel.save()
        coVerify(exactly = 1) { f.workspaceRepository.updatePropertiesPresentation("workspace-1", "Canonical edit", any(), any()) }
    }

    @Test
    fun `canonical blank route cannot fall back to restored legacy properties identity`() = runTest(dispatcher) {
        val f = fixture(snapshot(), saved = SavedStateHandle(mapOf("workspaceId" to "", "projectId" to "workspace-1")))
        assertThat(f.viewModel.uiState.value).isInstanceOf(WorkspacePropertiesUiState.Unavailable::class.java)
        io.mockk.verify(exactly = 0) { f.workspaceRepository.observeProperties(any()) }
    }

    @Test
    fun `independent binding refresh preserves unsaved scalar draft`() = runTest(dispatcher) {
        val bindings = MutableStateFlow<List<WorkspaceBindingEntity>>(emptyList())
        val f = fixture(snapshot(), bindings)
        f.viewModel.onNameChange("Unsaved")
        bindings.value = listOf(binding())
        assertThat(f.viewModel.ready().name).isEqualTo("Unsaved")
        assertThat(f.viewModel.ready().bindings).hasSize(1)
        coVerify(exactly = 0) { f.workspaceRepository.updatePropertiesPresentation(any(), any(), any(), any()) }
    }

    private fun fixture(
        snapshot: WorkspacePropertiesSnapshot?,
        bindings: MutableStateFlow<List<WorkspaceBindingEntity>> = MutableStateFlow(emptyList()),
        saved: SavedStateHandle = SavedStateHandle(mapOf("workspaceId" to "workspace-1")),
    ): Fixture {
        val workspaceRepository = mockk<CanonicalWorkspaceRepository>()
        val bindingRepository = mockk<CanonicalWorkspaceBindingRepository>()
        val catalogRepository = mockk<CanonicalEntityCatalogRepository>()
        val workspaceFlow = MutableStateFlow(snapshot)
        every { workspaceRepository.observeProperties("workspace-1") } returns workspaceFlow
        every { bindingRepository.observeBindingsForWorkspace("workspace-1") } returns bindings
        every { catalogRepository.observeCatalog() } returns MutableStateFlow(catalog())
        coEvery { workspaceRepository.updatePropertiesPresentation(any(), any(), any(), any()) } returns Unit
        coEvery { bindingRepository.bind(any(), any(), any(), any()) } returns "binding-new"
        coEvery { bindingRepository.unbind(any(), any()) } returns Unit
        return Fixture(
            viewModel =
                WorkspacePropertiesViewModel(
                    saved,
                    workspaceRepository,
                    bindingRepository,
                    catalogRepository,
                ),
            workspaceRepository = workspaceRepository,
            bindingRepository = bindingRepository,
            workspaceFlow = workspaceFlow,
        )
    }

    private fun WorkspacePropertiesViewModel.ready() =
        (uiState.value as WorkspacePropertiesUiState.Ready).model

    private fun snapshot(
        id: String = "workspace-1",
        sourceContextId: String? = null,
        isSystem: Boolean = false,
    ) = WorkspacePropertiesSnapshot(
        id = id,
        name = "Canonical Workspace",
        description = "Canonical description",
        roleCode = "PROJECT",
        sourceContextId = sourceContextId,
        isSystem = isSystem,
        version = 3L,
    )

    private fun binding() =
        WorkspaceBindingEntity(
            id = "binding-1",
            workspaceId = "workspace-1",
            subjectId = "aspect-1",
            bindingType = WorkspaceBindingType.SUPPORTS.name,
            isPrimary = false,
            bindingOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun catalog() =
        listOf(
            CanonicalEntitySummary(
                ref = CanonicalEntityRef.Aspect("aspect-1"),
                title = "Health",
            ),
            CanonicalEntitySummary(
                ref = CanonicalEntityRef.Orientation("orientation-1"),
                title = "Learn Kotlin",
                orientationKind = OrientationKind.GOAL,
            ),
        )

    private data class Fixture(
        val viewModel: WorkspacePropertiesViewModel,
        val workspaceRepository: CanonicalWorkspaceRepository,
        val bindingRepository: CanonicalWorkspaceBindingRepository,
        val workspaceFlow: MutableStateFlow<WorkspacePropertiesSnapshot?>,
    )
}
