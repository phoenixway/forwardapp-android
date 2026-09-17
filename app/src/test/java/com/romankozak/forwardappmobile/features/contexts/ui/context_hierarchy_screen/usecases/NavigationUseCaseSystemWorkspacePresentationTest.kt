package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.core.navigation.ClearCommand
import com.romankozak.forwardappmobile.core.navigation.ClearResult
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.data.workspace.projectPresentationUniverseFromState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NavigationUseCaseSystemWorkspacePresentationTest {
    @Test
    fun navigationUsesCanonicalNameFromProjectedHierarchySnapshot() =
        runTest {
            val id = SystemContexts.INBOX.raw
            val projected =
                projectPresentationUniverseFromState(
                    contexts = listOf(context(id, "stale-inbox")),
                    workspaces = listOf(workspace(id, "canonical-inbox")),
                    canonicalTagsById = mapOf(id to emptyList()),
                )

            val clearUseCase = mockk<ClearAndNavigateHomeUseCase>()
            coEvery { clearUseCase.execute(any(), any()) } returns ClearResult.Success
            val searchUseCase = mockk<SearchUseCase>(relaxed = true)
            every { searchUseCase.subStateStack } returns
                MutableStateFlow(listOf(ProjectHierarchyScreenSubState.Hierarchy))

            val useCase =
                NavigationUseCase(
                    clearAndNavigateHomeUseCase = clearUseCase,
                    searchUseCase = searchUseCase,
                    planningUseCase = mockk(relaxed = true),
                )

            useCase.attach(
                enhancedNavigationManager = mockk<EnhancedNavigationManager>(relaxed = true),
                uiEventChannel = Channel<ProjectUiEvent>(),
                hierarchyPresentationFlat =
                    MutableStateFlow(projected.map { it.toHierarchyPresentationNode() }),
            )

            useCase.onNavigateToProject(this, id)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                clearUseCase.execute(
                    command = ClearCommand.NavigateToProject(id, "canonical-inbox"),
                    context = any(),
                )
            }
        }

    private fun context(id: String, name: String) =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 2L,
        )

    private fun workspace(id: String, name: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = name,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )
}
