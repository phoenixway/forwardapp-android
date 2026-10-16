package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjection
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjectionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
        val repository = mockk<WorkspaceCompatibilityProjectionRepository>()
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
    fun `missing or tombstoned Workspace fails closed`() = runTest(dispatcher) {
        val repository = mockk<WorkspaceCompatibilityProjectionRepository>()
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
