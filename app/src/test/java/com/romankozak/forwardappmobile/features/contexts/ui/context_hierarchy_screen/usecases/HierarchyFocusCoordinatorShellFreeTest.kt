package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyReadAdapter
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.data.hierarchy.toCanonicalV2WorkspacePresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test

class HierarchyFocusCoordinatorShellFreeTest {
    @Test
    fun revealsShellFreeSystemByStableIdThroughOrientationBreadcrumbs() {
        val systemParent = presentation(SystemContexts.INBOX.raw, "Canonical Inbox")
        val system =
            presentation(
                id = SystemContexts.STRATEGIC.raw,
                name = "Canonical Direction",
            )
        val searchUseCase = mockk<SearchUseCase>(relaxed = true)
        val coordinator = HierarchyFocusCoordinator(searchUseCase)
        val breadcrumbs = slot<List<BreadcrumbItem>>()
        val orientation =
            listOf(
                OrientationHierarchyItem(OrientationHierarchyNode.NoBeacon, level = 0),
                OrientationHierarchyItem(
                    OrientationHierarchyNode.WorkspaceNode(
                        presentation = systemParent,
                        linkedBeaconIds = emptySet(),
                    ),
                    level = 1,
                ),
                OrientationHierarchyItem(
                    OrientationHierarchyNode.WorkspaceNode(
                        presentation = system,
                        linkedBeaconIds = emptySet(),
                    ),
                    level = 2,
                ),
            )

        val canonicalRead =
            CanonicalV2ProductionHierarchyReadAdapter().read(
                placements =
                    listOf(
                        placement(
                            id = "system-parent-placement",
                            target = workspaceTarget(systemParent.id),
                            order = 0,
                        ),
                        placement(
                            id = "system-placement",
                            target = workspaceTarget(system.id),
                            parentId = "system-parent-placement",
                            order = 0,
                        ),
                    ),
                admittedWorkspacePresentations =
                    listOf(systemParent, system).map {
                        it.toCanonicalV2WorkspacePresentation()
                    },
                managedSubjects = emptyList(),
                syntheticScopes = emptyList(),
            )

        coordinator.revealProject(
            projectId = system.id,
            placementId = "system-placement",
            currentHierarchy = HierarchyPresentationData(allProjects = listOf(system)),
            currentSubState = ProjectHierarchyScreenSubState.Hierarchy,
            currentBreadcrumbs = emptyList(),
            orientationHierarchy = orientation,
            canonicalRead = canonicalRead,
            enterFocus = true,
            replaceFocusPath = true,
        )

        verify {
            searchUseCase.navigateToProjectWithBreadcrumbs(
                projectId = system.id,
                breadcrumbs = capture(breadcrumbs),
            )
        }
        assertEquals(
            canonicalRead.breadcrumbsToOccurrence(PlacementId("system-placement")).map { it.id },
            breadcrumbs.captured.map { it.id },
        )
        assertEquals(BreadcrumbTarget.Context, breadcrumbs.captured.last().target)
        verify {
            searchUseCase.enterProjectFocusPath(
                projectId = system.id,
                breadcrumbs = any(),
                placementId = "system-placement",
            )
        }
    }

    @Test
    fun exactPlacementRevealFailsClosedInsteadOfFallingBackToTargetNavigation() {
        val visible = presentation(SystemContexts.STRATEGIC.raw, "Canonical Direction")
        val searchUseCase = mockk<SearchUseCase>(relaxed = true)
        val coordinator = HierarchyFocusCoordinator(searchUseCase)
        val orientation =
            listOf(
                OrientationHierarchyItem(
                    OrientationHierarchyNode.WorkspaceNode(
                        presentation = visible,
                        linkedBeaconIds = emptySet(),
                    ),
                    level = 0,
                ),
            )

        coordinator.revealProject(
            projectId = visible.id,
            placementId = "missing-placement",
            currentHierarchy = HierarchyPresentationData(allProjects = listOf(visible)),
            currentSubState = ProjectHierarchyScreenSubState.Hierarchy,
            currentBreadcrumbs = emptyList(),
            orientationHierarchy = orientation,
            enterFocus = true,
            replaceFocusPath = true,
        )

        verify(exactly = 0) {
            searchUseCase.navigateToProject(
                projectId = any(),
            )
        }
        verify(exactly = 0) {
            searchUseCase.navigateToProjectWithBreadcrumbs(
                projectId = any(),
                breadcrumbs = any(),
            )
        }
        verify(exactly = 0) {
            searchUseCase.enterProjectFocus(
                projectId = any(),
                placementId = any(),
            )
        }
        verify(exactly = 0) {
            searchUseCase.enterProjectFocusPath(
                projectId = any(),
                breadcrumbs = any(),
                placementId = any(),
            )
        }
    }

    @Test
    fun unknownIdFailsClosedWithoutNavigationOrFocus() {
        val visible = presentation(SystemContexts.INBOX.raw, "Canonical Inbox")
        val searchUseCase = mockk<SearchUseCase>(relaxed = true)
        val coordinator = HierarchyFocusCoordinator(searchUseCase)

        coordinator.revealProject(
            projectId = "unknown-project",
            currentHierarchy =
                HierarchyPresentationData(
                    allProjects = listOf(visible),
                    topLevelProjects = listOf(visible),
                ),
            currentSubState = ProjectHierarchyScreenSubState.Hierarchy,
            currentBreadcrumbs = emptyList(),
            orientationHierarchy = emptyList(),
            enterFocus = true,
            replaceFocusPath = true,
        )

        verify(exactly = 0) {
            searchUseCase.navigateToProject(
                projectId = any(),
            )
        }
        verify(exactly = 0) {
            searchUseCase.navigateToProjectWithBreadcrumbs(
                projectId = any(),
                breadcrumbs = any(),
            )
        }
        verify(exactly = 0) {
            searchUseCase.enterProjectFocus(any())
        }
        verify(exactly = 0) {
            searchUseCase.enterProjectFocusPath(
                projectId = any(),
                breadcrumbs = any(),
                placementId = "system-placement",
            )
        }
    }

    private fun presentation(id: String, name: String) =
        HierarchyContextPresentationNode(
            id = id,
            name = name,
            description = null,
            parentId = null,
            order = 0L,
            roleCode = null,
            tags = emptyList(),
        )

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

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

}
