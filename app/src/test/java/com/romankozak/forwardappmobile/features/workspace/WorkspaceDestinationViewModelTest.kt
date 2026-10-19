package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjection
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceDestinationViewModelTest {
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
    fun `standalone Workspace loads from canonical identity without Context`() = runTest(dispatcher) {
        val repository = repository()
        coEvery { repository.get("workspace-only") } returns projection(workspace("workspace-only", null))

        val viewModel = WorkspaceDestinationViewModel(
            SavedStateHandle(mapOf("workspaceId" to "workspace-only")),
            repository,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isEqualTo(
                WorkspaceDestinationUiState.Ready(
                    workspaceId = "workspace-only",
                    title = "Canonical title",
                    description = "Canonical description",
                    roleCode = "PROJECT",
                    compatibilityContextId = null,
                ),
            )
    }

    @Test
    fun `explicit live compatibility Context enables existing operational surface`() {
        val state =
            projection(
                workspace = workspace("context-backed", "context-backed"),
                sourceContext = context("context-backed"),
            ).toDestinationState()

        assertThat((state as WorkspaceDestinationUiState.Ready).compatibilityContextId)
            .isEqualTo("context-backed")
    }

    @Test
    fun `operational screen presentation admits standalone retired shell and System Workspace`() {
        val cases = listOf(
            projection(workspace("standalone", null)),
            projection(workspace("retired", null).copy(provenance = "CANONICAL_ONLY"),
                sourceContext = context("retired").copy(isDeleted = true)),
            projection(workspace(SystemContexts.INBOX.raw, null).copy(provenance = "CANONICAL_ONLY")),
        )
        cases.forEach { source ->
            val ready = source.toDestinationState() as WorkspaceDestinationUiState.Ready
            assertThat(ready.compatibilityContextId).isNull()
            val presentation = ready.toContextPresentation()
            assertThat(presentation.id).isEqualTo(source.workspace.id)
            assertThat(presentation.name).isEqualTo("Canonical title")
            assertThat(presentation.description).isEqualTo("Canonical description")
            assertThat(presentation.roleCode).isEqualTo("PROJECT")
            assertThat(presentation.parentId).isNull()
        }
    }

    @Test
    fun `operational presentation never substitutes compatibility Context for Workspace identity`() {
        val ready = WorkspaceDestinationUiState.Ready(
            workspaceId = "canonical-workspace", title = "Canonical title", description = null,
            roleCode = null, compatibilityContextId = "legacy-context",
        )
        assertThat(ready.toContextPresentation().id).isEqualTo("canonical-workspace")
    }

    @Test
    fun `missing or tombstoned Workspace fails closed`() = runTest(dispatcher) {
        val repository = repository()
        coEvery { repository.get("missing") } returns null
        val missing = WorkspaceDestinationViewModel(
            SavedStateHandle(mapOf("workspaceId" to "missing")),
            repository,
        )
        advanceUntilIdle()
        assertThat(missing.uiState.value).isInstanceOf(WorkspaceDestinationUiState.Unavailable::class.java)

        val deletedState = projection(workspace("deleted", null).copy(isDeleted = true)).toDestinationState()
        assertThat(deletedState).isInstanceOf(WorkspaceDestinationUiState.Unavailable::class.java)
    }

    @Test
    fun `same-id Context is not admitted without explicit source provenance`() {
        val state =
            projection(
                workspace = workspace("standalone", null),
                sourceContext = context("standalone"),
            ).toDestinationState()

        assertThat((state as WorkspaceDestinationUiState.Ready).compatibilityContextId).isNull()
    }

    @Test
    fun `Context-backed route reads canonical Workspace only and restores the same identity`() = runTest(dispatcher) {
        val repository = repository()
        coEvery { repository.get("workspace") } returns projection(workspace("workspace", "workspace"), context("workspace"))
        val saved = SavedStateHandle(mapOf("workspaceId" to "workspace", "listId" to "stale-context"))
        repeat(2) {
            val vm = WorkspaceDestinationViewModel(saved, repository)
            advanceUntilIdle()
            val ready = vm.uiState.value as WorkspaceDestinationUiState.Ready
            assertThat(ready.workspaceId).isEqualTo("workspace")
            assertThat(ready.title).isEqualTo("Canonical title")
            assertThat(ready.compatibilityContextId).isEqualTo("workspace")
        }
        coVerify(exactly = 2) { repository.get("workspace") }
        verify(exactly = 2) { repository.observeWorkspace("workspace") }
        confirmVerified(repository)
    }

    @Test
    fun `missing canonical route argument never loads legacy Context as substitute`() = runTest(dispatcher) {
        val repository = repository()
        val vm = WorkspaceDestinationViewModel(SavedStateHandle(mapOf("listId" to "legacy")), repository)
        advanceUntilIdle()
        assertThat(vm.uiState.value).isInstanceOf(WorkspaceDestinationUiState.Unavailable::class.java)
        coVerify(exactly = 0) { repository.get(any()) }
    }

    @Test
    fun `explicit differently keyed source never changes canonical identity or title`() {
        val ready = projection(workspace("canonical", "legacy"), context("legacy"))
            .toDestinationState() as WorkspaceDestinationUiState.Ready
        assertThat(ready.workspaceId).isEqualTo("canonical")
        assertThat(ready.title).isEqualTo("Canonical title")
        // The current legacy action adapter supports only exact same-key shells.
        assertThat(ready.compatibilityContextId).isNull()
    }

    @Test
    fun `Properties updates and tombstone are observed from canonical Workspace not Context`() = runTest(dispatcher) {
        val repository = repository()
        val initial = workspace("canonical", "canonical")
        val rows = MutableStateFlow<WorkspaceEntity?>(initial)
        coEvery { repository.get("canonical") } returns projection(initial, context("canonical"))
        every { repository.observeWorkspace("canonical") } returns rows
        val vm = WorkspaceDestinationViewModel(SavedStateHandle(mapOf("workspaceId" to "canonical")), repository)
        advanceUntilIdle()
        rows.value = initial.copy(nameOverride = "Saved canonical title", descriptionOverride = "Saved description", version = 2)
        advanceUntilIdle()
        val ready = vm.uiState.value as WorkspaceDestinationUiState.Ready
        assertThat(ready.title).isEqualTo("Saved canonical title")
        assertThat(ready.description).isEqualTo("Saved description")
        assertThat(ready.workspaceId).isEqualTo("canonical")
        rows.value = rows.value!!.copy(isDeleted = true, version = 3)
        advanceUntilIdle()
        assertThat(vm.uiState.value).isInstanceOf(WorkspaceDestinationUiState.Unavailable::class.java)
    }

    private fun repository() = mockk<WorkspaceCompatibilityProjectionRepository>().also {
        every { it.observeWorkspace(any()) } returns emptyFlow()
    }

    private fun workspace(id: String, sourceContextId: String?) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Canonical title",
            descriptionOverride = "Canonical description",
            roleCode = "PROJECT",
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = if (sourceContextId == null) "STANDALONE" else "CONTEXT_BACKED",
            sourceContextId = sourceContextId,
        )

    private fun context(id: String) =
        Context(
            id = id,
            name = "Legacy title must not own destination",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 2L,
        )

    private fun projection(
        workspace: WorkspaceEntity,
        sourceContext: Context? = null,
    ) = WorkspaceCompatibilityProjection(
        workspace = workspace,
        sourceContext = sourceContext,
        capabilities = emptyList(),
        diagnostics = emptyList(),
    )
}
