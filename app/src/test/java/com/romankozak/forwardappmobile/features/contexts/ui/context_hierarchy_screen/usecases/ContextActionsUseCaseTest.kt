package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommandService
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommand
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyReadAdapter
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyOccurrenceRef
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRolePresetInitializer
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.DirectionCapabilityState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.sync.SyncRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextActionsUseCaseTest {
    private val contextRepository = mockk<ContextRepository>(relaxed = true)
    private val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)
    private val hierarchyOccurrenceCommandService = mockk<HierarchyOccurrenceCommandService>(relaxed = true)
    private val canonicalWorkspaceRolePresetInitializer =
        mockk<CanonicalWorkspaceRolePresetInitializer>(relaxed = true)
    private val canonicalDirectionRepository = mockk<CanonicalDirectionRepository>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

    private val useCase =
        ContextActionsUseCase(
            contextRepository = contextRepository,
            hierarchyOccurrenceCommandService = hierarchyOccurrenceCommandService,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            canonicalWorkspaceRolePresetInitializer = canonicalWorkspaceRolePresetInitializer,
            canonicalDirectionRepository = canonicalDirectionRepository,
            syncRepository = syncRepository,
            settingsRepository = settingsRepository,
            mainBeaconRepository = mainBeaconRepository,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

    @Test
    fun addNewProjectCreatesStandaloneWorkspaceAndReturnsCanonicalId() = runTest {
        coEvery {
            canonicalWorkspaceRepository.createWithPrimaryAppearance(
                nameOverride = "Operations",
                descriptionOverride = null,
                parentWorkspaceId = "parent-workspace",
                roleCode = "management",
                now = any(),
            )
        } returns "workspace-generated-id"

        coEvery { canonicalDirectionRepository.getState("parent-workspace") } returns
            DirectionCapabilityState(
                lifecycleState = WorkspaceCapabilityState.ACTIVE,
                isDeleted = false,
                configuration = DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = true),
            )

        val result =
            useCase.addNewProject(
                parentId = "parent-workspace",
                parentPlacementId = null,
                name = "  Operations  ",
                roleCode = "management",
            )

        assertEquals("workspace-generated-id", result)
        coVerify(exactly = 1) {
            canonicalWorkspaceRepository.createWithPrimaryAppearance(
                nameOverride = "Operations",
                descriptionOverride = null,
                parentWorkspaceId = "parent-workspace",
                roleCode = "management",
                now = any(),
            )
        }
        coVerify(exactly = 1) {
            canonicalWorkspaceRolePresetInitializer.apply(
                workspaceId = "workspace-generated-id",
                roleCode = "management",
                now = any(),
            )
        }
        coVerify(exactly = 1) {
            canonicalDirectionRepository.createWorkspaceLinkAtFront(
                workspaceId = "parent-workspace",
                targetWorkspaceId = "workspace-generated-id",
                label = "Operations",
                now = any(),
            )
        }
    }

    @Test
    fun addNewProjectDoesNotAutoLinkWhenParentDirectionPolicyIsDisabled() = runTest {
        coEvery {
            canonicalWorkspaceRepository.createWithPrimaryAppearance(
                nameOverride = "Child",
                descriptionOverride = null,
                parentWorkspaceId = "parent-workspace",
                roleCode = null,
                now = any(),
            )
        } returns "child-workspace"

        coEvery { canonicalDirectionRepository.getState("parent-workspace") } returns
            DirectionCapabilityState(
                lifecycleState = WorkspaceCapabilityState.ACTIVE,
                isDeleted = false,
                configuration = DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false),
            )

        val result =
            useCase.addNewProject(
                parentId = "parent-workspace",
                parentPlacementId = null,
                name = "Child",
                roleCode = null,
            )

        assertEquals("child-workspace", result)
    }

    @Test
    fun addNewProjectBlankNameAuthorsNothing() = runTest {
        val result =
            useCase.addNewProject(
                parentId = null,
                parentPlacementId = null,
                name = "   ",
                roleCode = "management",
            )

        assertNull(result)
        coVerify(exactly = 0) {
            canonicalWorkspaceRepository.createWithPrimaryAppearance(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            canonicalWorkspaceRolePresetInitializer.apply(any(), any(), any())
        }
    }

    @Test
    fun getMoveProjectRouteUsesOccurrenceTopologyAndDisablesDescendants() {
        val read =
            v2Read(
                placements =
                    listOf(
                        placement("root-placement", "root", nameOrder = 0),
                        placement(
                            "source-placement",
                            "source",
                            parentId = "root-placement",
                            nameOrder = 0,
                        ),
                        placement(
                            "child-placement",
                            "child",
                            parentId = "source-placement",
                            nameOrder = 0,
                        ),
                        placement(
                            "grandchild-placement",
                            "grandchild",
                            parentId = "child-placement",
                            nameOrder = 0,
                        ),
                    ),
                presentations =
                    listOf(
                        workspacePresentation("root", "Root"),
                        workspacePresentation("source", "Source"),
                        workspacePresentation("child", "Child"),
                        workspacePresentation("grandchild", "Grandchild"),
                    ),
            )
        val occurrence =
            requireNotNull(read.occurrence(PlacementId("source-placement")))
                .toHierarchyOccurrenceRef()

        val route = requireNotNull(useCase.getMoveProjectRoute(occurrence, read))

        assertEquals("Move 'Source'", route.title)
        assertEquals("root-placement", route.currentParentId)
        assertEquals(
            setOf("source-placement", "child-placement", "grandchild-placement"),
            route.disabledIds?.split(",")?.toSet(),
        )
    }

    @Test
    fun getMoveProjectRouteFailsClosedWhenPlacementIsMissing() {
        val read =
            v2Read(
                placements = listOf(placement("root-placement", "root")),
                presentations = listOf(workspacePresentation("root", "Root")),
            )
        val missing =
            HierarchyOccurrenceRef(
                placementId = PlacementId("missing-placement"),
                target = workspaceTarget("missing"),
                parentPlacementId = null,
                placementKind = PlacementKind.PRIMARY,
                siblingOrder = 0,
            )

        assertNull(useCase.getMoveProjectRoute(missing, read))
    }

    @Test
    fun deleteDelegatesSubtreeTombstoningToCanonicalWorkspaceOwner() = runTest {
        useCase.onDeleteProjectConfirmed(projectId = "root")

        coVerify(exactly = 1) {
            canonicalWorkspaceRepository.tombstoneSubtree(
                rootId = "root",
                now = any(),
            )
        }
    }

    @Test
    fun deleteDoesNotUseLegacyContextSubtreeMutation() = runTest {
        useCase.onDeleteProjectConfirmed(projectId = "root")

        coVerify(exactly = 0) {
            contextRepository.deleteContextsByIds(any())
        }
    }

    @Test
    fun reorderContextSiblingsDelegatesToHierarchyOccurrenceWriter() = runTest {
        val parentPlacementId = PlacementId("parent-placement")
        val childPlacementId = PlacementId("child-placement")

        useCase.reorderContextSiblings(
            parentPlacementId = parentPlacementId,
            orderedPlacementIds = listOf(childPlacementId),
        )

        coVerify(exactly = 1) {
            hierarchyOccurrenceCommandService.reorderSiblings(
                HierarchyOccurrenceCommand.ReorderSiblings(
                    parentPlacementId = parentPlacementId,
                    orderedPlacementIds = listOf(childPlacementId),
                ),
                any(),
            )
        }
    }

    private fun v2Read(
        placements: List<HierarchyPlacement>,
        presentations: List<HierarchyContextPresentationNode>,
    ) =
        CanonicalV2ProductionHierarchyReadAdapter().read(
            placements = placements,
            admittedWorkspacePresentations = presentations,
            managedSubjects = emptyList(),
        )

    private fun placement(
        placementId: String,
        workspaceId: String,
        parentId: String? = null,
        nameOrder: Long = 0,
    ) = HierarchyPlacement(
        id = PlacementId(placementId),
        hierarchyId = HierarchyId.GENERAL,
        target = workspaceTarget(workspaceId),
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = PlacementKind.PRIMARY,
        siblingOrder = nameOrder,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun workspacePresentation(
        id: String,
        name: String,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = null,
        parentId = null,
        order = 0,
        roleCode = null,
        tags = emptyList(),
    )

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun context(
        id: String,
        parentId: String? = null,
        name: String = id,
        order: Long = 0,
    ): Context =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            order = order,
        )
}
