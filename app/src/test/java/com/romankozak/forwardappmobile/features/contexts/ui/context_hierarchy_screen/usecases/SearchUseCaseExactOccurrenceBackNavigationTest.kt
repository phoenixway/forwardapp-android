package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.hierarchy.CANONICAL_V2_NO_GROUP_SCOPE_ID
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyReadAdapter
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2SyntheticScopeInput
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2SyntheticScopeKind
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.data.hierarchy.toCanonicalV2WorkspacePresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
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
            val read = duplicateWorkspaceRead(shared)
            val useCase = initializedSearchUseCase(shared, read)

            val firstBreadcrumbs =
                read.breadcrumbsToOccurrence(PlacementId("workspace-placement-a"))
                    .map { breadcrumb ->
                        BreadcrumbItem(
                            id = breadcrumb.id,
                            name = breadcrumb.title,
                            level = breadcrumb.level,
                            target =
                                when (breadcrumb.target) {
                                    com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget.CONTEXT ->
                                        BreadcrumbTarget.Context
                                    com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget.ORIENTATION_NODE ->
                                        BreadcrumbTarget.OrientationNode
                                },
                            placementId = breadcrumb.placementId?.value,
                        )
                    }
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
                canonicalRead = read,
                goBack = {},
            )
            advanceUntilIdle()

            assertEquals(
                listOf(CANONICAL_V2_NO_GROUP_SCOPE_ID, "beacon-a", "shared"),
                useCase.currentBreadcrumbs.value.map { it.id },
            )
            assertEquals(
                listOf(null, "beacon-placement-a", "workspace-placement-a"),
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
            val fullRead = duplicateWorkspaceRead(shared)
            val remainingRead =
                duplicateWorkspaceRead(
                    shared = shared,
                    includePlacementA = false,
                )
            val useCase = initializedSearchUseCase(shared, fullRead)

            val firstBreadcrumbs =
                fullRead.breadcrumbsToOccurrence(PlacementId("workspace-placement-a"))
                    .map { breadcrumb ->
                        BreadcrumbItem(
                            id = breadcrumb.id,
                            name = breadcrumb.title,
                            level = breadcrumb.level,
                            target =
                                when (breadcrumb.target) {
                                    com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget.CONTEXT ->
                                        BreadcrumbTarget.Context
                                    com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget.ORIENTATION_NODE ->
                                        BreadcrumbTarget.OrientationNode
                                },
                            placementId = breadcrumb.placementId?.value,
                        )
                    }
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
                canonicalRead = remainingRead,
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

    @Test
    fun reconciliationFailsClosedWhenFocusedExactOccurrenceDisappears() =
        runTest {
            val shared = presentation("shared", "Shared Workspace")
            val fullRead = duplicateWorkspaceRead(shared)
            val canonicalRead = MutableStateFlow<CanonicalV2ProductionHierarchyRead?>(fullRead)
            val useCase =
                SearchUseCase(
                    recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true),
                    savedStateHandle = SavedStateHandle(),
                )
            useCase.initialize(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
                uiEventChannel = Channel<ProjectUiEvent>(),
                onProjectAccess = {},
                canonicalV2Read = canonicalRead,
            )
            val exactBreadcrumbs =
                fullRead.breadcrumbsToOccurrence(PlacementId("workspace-placement-a"))
                    .map { breadcrumb ->
                        BreadcrumbItem(
                            id = breadcrumb.id,
                            name = breadcrumb.title,
                            level = breadcrumb.level,
                            target =
                                when (breadcrumb.target) {
                                    com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget.CONTEXT ->
                                        BreadcrumbTarget.Context
                                    com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget.ORIENTATION_NODE ->
                                        BreadcrumbTarget.OrientationNode
                                },
                            placementId = breadcrumb.placementId?.value,
                        )
                    }
            useCase.enterProjectFocusPath(
                projectId = shared.id,
                breadcrumbs = exactBreadcrumbs,
                placementId = "workspace-placement-a",
            )
            useCase.currentBreadcrumbs.value = exactBreadcrumbs

            canonicalRead.value = duplicateWorkspaceRead(shared, includePlacementA = false)
            useCase.reconcileFocusedProjectBreadcrumbs()
            advanceUntilIdle()

            assertEquals(
                listOf(ProjectHierarchyScreenSubState.Hierarchy),
                useCase.subStateStack.value,
            )
            assertEquals(emptyList<BreadcrumbItem>(), useCase.currentBreadcrumbs.value)
            assertNull(useCase.focusedProjectId.value)
        }

    @Test
    fun targetOnlyNavigationUsesFirstVisibleCanonicalOccurrenceWithoutLegacyPrefix() =
        runTest {
            val shared = presentation("shared", "Shared Workspace")
            val read = duplicateWorkspaceRead(shared)
            val useCase = initializedSearchUseCase(shared, read)

            useCase.navigateToProject(shared.id)
            advanceUntilIdle()

            assertEquals(
                listOf(CANONICAL_V2_NO_GROUP_SCOPE_ID, "beacon-a", shared.id),
                useCase.currentBreadcrumbs.value.map { it.id },
            )
            assertEquals(
                listOf(null, "beacon-placement-a", "workspace-placement-a"),
                useCase.currentBreadcrumbs.value.map { it.placementId },
            )
            assertEquals(shared.id, useCase.focusedProjectId.value)

            useCase.clearNavigation()
            useCase.navigateToProject("missing-workspace")
            advanceUntilIdle()

            assertEquals(emptyList<BreadcrumbItem>(), useCase.currentBreadcrumbs.value)
            assertNull(useCase.focusedProjectId.value)
        }

    private fun initializedSearchUseCase(
        shared: HierarchyContextPresentationNode,
        read: CanonicalV2ProductionHierarchyRead,
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
            canonicalV2Read = MutableStateFlow(read),
        )
        return useCase
    }

    private fun duplicateWorkspaceRead(
        shared: HierarchyContextPresentationNode,
        includePlacementA: Boolean = true,
    ): CanonicalV2ProductionHierarchyRead {
        val placements =
            buildList {
                if (includePlacementA) {
                    add(
                        placement(
                            id = "beacon-placement-a",
                            target = subjectTarget("beacon-a"),
                            order = 0,
                        ),
                    )
                    add(
                        placement(
                            id = "workspace-placement-a",
                            target = workspaceTarget(shared.id),
                            parentId = "beacon-placement-a",
                            kind = PlacementKind.LINK,
                        ),
                    )
                }
                add(
                    placement(
                        id = "beacon-placement-b",
                        target = subjectTarget("beacon-b"),
                        order = 1,
                    ),
                )
                add(
                    placement(
                        id = "workspace-placement-b",
                        target = workspaceTarget(shared.id),
                        parentId = "beacon-placement-b",
                        kind = PlacementKind.LINK,
                    ),
                )
            }

        val rootPlacementIds =
            buildList {
                if (includePlacementA) add(PlacementId("beacon-placement-a"))
                add(PlacementId("beacon-placement-b"))
            }

        return CanonicalV2ProductionHierarchyReadAdapter().read(
            placements = placements,
            admittedWorkspacePresentations =
                listOf(shared.toCanonicalV2WorkspacePresentation()),
            managedSubjects =
                buildList {
                    if (includePlacementA) add(subject("beacon-a", "Beacon A"))
                    add(subject("beacon-b", "Beacon B"))
                },
            syntheticScopes =
                listOf(
                    CanonicalV2SyntheticScopeInput(
                        kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                        id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                        title = "No group",
                        order = 0,
                        rootPlacementIds = rootPlacementIds,
                    ),
                ),
        )
    }

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = HierarchyId.GENERAL,
        target = target,
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun subject(
        id: String,
        title: String,
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = "ORIENTATION",
        title = title,
        description = null,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

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
