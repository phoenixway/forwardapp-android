package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchUseCaseExactOccurrenceBackNavigationTest {
    @Test
    fun backRestoresPreviousExactWorkspaceOccurrenceInsteadOfTargetOnlyPath() =
        runTest {
            val shared = presentation("shared", "Shared Workspace")
            val orientation = duplicateWorkspaceOrientation(shared)
            val useCase = initializedSearchUseCase(shared)

            val firstBreadcrumbs =
                buildOrientationBreadcrumbsToContext(
                    items = orientation,
                    contextId = shared.id,
                    placementId = "workspace-placement-a",
                )
            useCase.enterProjectFocusPath(
                projectId = shared.id,
                breadcrumbs = firstBreadcrumbs,
                placementId = "workspace-placement-a",
            )
            useCase.enterProjectFocus(
                projectId = shared.id,
                placementId = "workspace-placement-b",
            )

            useCase.handleBackNavigation(
                currentHierarchy = HierarchyPresentationData(allProjects = listOf(shared)),
                orientationHierarchy = orientation,
                goBack = {},
            )
            advanceUntilIdle()

            assertEquals(
                listOf("beacon-a", "shared"),
                useCase.currentBreadcrumbs.value.map { it.id },
            )
            assertEquals(
                listOf("beacon-placement-a", "workspace-placement-a"),
                useCase.currentBreadcrumbs.value.map { it.placementId },
            )
            assertEquals(
                ProjectHierarchyScreenSubState.ProjectFocused(
                    projectId = shared.id,
                    placementId = "workspace-placement-a",
                ),
                useCase.subStateStack.value.last(),
            )
            assertEquals(shared.id, useCase.focusedProjectId.value)
        }

    @Test
    fun backFailsClosedWhenPreviousExactWorkspaceOccurrenceNoLongerExists() =
        runTest {
            val shared = presentation("shared", "Shared Workspace")
            val fullOrientation = duplicateWorkspaceOrientation(shared)
            val remainingOrientation =
                fullOrientation.filterNot { item ->
                    item.node.placementId?.value == "workspace-placement-a"
                }
            val useCase = initializedSearchUseCase(shared)

            val firstBreadcrumbs =
                buildOrientationBreadcrumbsToContext(
                    items = fullOrientation,
                    contextId = shared.id,
                    placementId = "workspace-placement-a",
                )
            useCase.enterProjectFocusPath(
                projectId = shared.id,
                breadcrumbs = firstBreadcrumbs,
                placementId = "workspace-placement-a",
            )
            useCase.enterProjectFocus(
                projectId = shared.id,
                placementId = "workspace-placement-b",
            )

            useCase.handleBackNavigation(
                currentHierarchy = HierarchyPresentationData(allProjects = listOf(shared)),
                orientationHierarchy = remainingOrientation,
                goBack = {},
            )
            advanceUntilIdle()

            assertEquals(
                listOf(ProjectHierarchyScreenSubState.Hierarchy),
                useCase.subStateStack.value,
            )
            assertEquals(emptyList<BreadcrumbItem>(), useCase.currentBreadcrumbs.value)
            assertNull(useCase.focusedProjectId.value)
        }

    private fun initializedSearchUseCase(
        shared: HierarchyContextPresentationNode,
    ): SearchUseCase {
        val useCase =
            SearchUseCase(
                recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true),
                savedStateHandle = SavedStateHandle(),
            )
        useCase.initialize(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            uiEventChannel = Channel<ProjectUiEvent>(),
            onProjectAccess = {},
            hierarchyPresentationFlat = MutableStateFlow(listOf(shared)),
        )
        return useCase
    }

    private fun duplicateWorkspaceOrientation(
        shared: HierarchyContextPresentationNode,
    ): List<OrientationHierarchyItem> =
        listOf(
            OrientationHierarchyItem(
                node = beacon("beacon-a", "Beacon A", "beacon-placement-a"),
                level = 0,
            ),
            OrientationHierarchyItem(
                node = workspace(shared, "workspace-placement-a"),
                level = 1,
            ),
            OrientationHierarchyItem(
                node = beacon("beacon-b", "Beacon B", "beacon-placement-b"),
                level = 0,
            ),
            OrientationHierarchyItem(
                node = workspace(shared, "workspace-placement-b"),
                level = 1,
            ),
        )

    private fun presentation(
        id: String,
        name: String,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = null,
        parentId = null,
        order = 0L,
        roleCode = null,
        tags = emptyList(),
    )

    private fun workspace(
        presentation: HierarchyContextPresentationNode,
        placementId: String,
    ) = OrientationHierarchyNode.WorkspaceNode(
        presentation = presentation,
        linkedBeaconIds = emptySet(),
        placementId = PlacementId(placementId),
    )

    private fun beacon(
        id: String,
        title: String,
        placementId: String,
    ) = OrientationHierarchyNode.Beacon(
        id = id,
        title = title,
        readinessStatus = MainBeaconReadinessStatus.READY,
        relatedContextCount = 0,
        placementId = PlacementId(placementId),
    )
}
