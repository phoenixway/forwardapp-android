package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.GoalOrientationBridge
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogConfigurationAccess
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogConfigurationState
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider

class GoalRepositoryCanonicalBacklogPolicyTest {
    @Test
    fun `System canonical Backlog false beats contradictory legacy true`() = runTest {
        val goalDao = mockk<GoalDao>(relaxed = true)
        val associations = mockk<TagAssociationHandler>()
        val placements = mockk<BacklogPlacementCommands>()
        val structures = mockk<ContextStructureRepository>()
        val access = mockk<SystemContextCanonicalBacklogConfigurationAccess>()
        val goal = Goal(id = "goal", text = "#tag", completed = false, createdAt = 1L, updatedAt = 2L)
        coEvery {
            placements.findFirstContextBackedWorkspaceId(itemType = "GOAL", entityId = goal.id)
        } returns null
        coEvery { associations.findGoalAssociationOwnerContextId(goal.id) } returns "sys_inbox"
        coEvery { associations.syncGoalAssociations(any(), "sys_inbox") } returns mapOf("associated" to "tag")
        coEvery {
            placements.hasContextBackedPlacementHistory("sys_inbox", "GOAL", goal.id)
        } returns true
        coEvery { structures.getStructureByContext("sys_inbox") } returns
            com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration.default("sys_inbox")
                .copy(removeBacklogEntryAfterTagAutocopy = true)
        coEvery { access.getState("sys_inbox") } returns
            SystemBacklogConfigurationState(
                configuration = BacklogCapabilityConfigurationV2(false),
                isEstablished = true,
            )
        coEvery {
            placements.setContextBackedPlacementVisible(
                contextId = any(),
                itemType = any(),
                entityId = any(),
                visible = any(),
                now = any(),
            )
        } returns "placement"

        repository(goalDao, associations, placements, structures, access).updateGoal(goal)

        coVerify {
            placements.setContextBackedPlacementVisible(
                contextId = "sys_inbox",
                itemType = "GOAL",
                entityId = goal.id,
                visible = true,
                now = any(),
            )
        }
    }

    @Test
    fun `System canonical Backlog true beats contradictory legacy false`() = runTest {
        val goalDao = mockk<GoalDao>(relaxed = true)
        val associations = mockk<TagAssociationHandler>()
        val placements = mockk<BacklogPlacementCommands>()
        val structures = mockk<ContextStructureRepository>()
        val access = mockk<SystemContextCanonicalBacklogConfigurationAccess>()
        val goal = Goal(id = "goal", text = "#tag", completed = false, createdAt = 1L, updatedAt = 2L)
        coEvery {
            placements.findFirstContextBackedWorkspaceId(itemType = "GOAL", entityId = goal.id)
        } returns null
        coEvery { associations.findGoalAssociationOwnerContextId(goal.id) } returns "sys_inbox"
        coEvery { associations.syncGoalAssociations(any(), "sys_inbox") } returns mapOf("associated" to "tag")
        coEvery { placements.hasContextBackedPlacementHistory("sys_inbox", "GOAL", goal.id) } returns true
        coEvery { structures.getStructureByContext("sys_inbox") } returns
            com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration.default("sys_inbox")
                .copy(removeBacklogEntryAfterTagAutocopy = false)
        coEvery { access.getState("sys_inbox") } returns
            SystemBacklogConfigurationState(
                configuration = BacklogCapabilityConfigurationV2(true),
                isEstablished = true,
            )
        coEvery {
            placements.setContextBackedPlacementVisible(
                contextId = any(),
                itemType = any(),
                entityId = any(),
                visible = any(),
                now = any(),
            )
        } returns "placement"

        repository(goalDao, associations, placements, structures, access).updateGoal(goal)

        coVerify {
            placements.setContextBackedPlacementVisible(
                contextId = "sys_inbox",
                itemType = "GOAL",
                entityId = goal.id,
                visible = false,
                now = any(),
            )
        }
    }

    @Test
    fun `editing auto-hidden Goal restores source placement after hashtag removal`() = runTest {
        val goalDao = mockk<GoalDao>(relaxed = true)
        val associations = mockk<TagAssociationHandler>()
        val placements = mockk<BacklogPlacementCommands>()
        val structures = mockk<ContextStructureRepository>()
        val goal =
            Goal(
                id = "goal",
                text = "without hashtag",
                completed = false,
                createdAt = 1L,
                updatedAt = 2L,
            )

        coEvery {
            placements.findFirstContextBackedWorkspaceId(
                itemType = "GOAL",
                entityId = goal.id,
            )
        } returns null
        coEvery { associations.findGoalAssociationOwnerContextId(goal.id) } returns "source-context"
        coEvery { associations.syncGoalAssociations(any(), "source-context") } returns emptyMap()
        coEvery { placements.hasContextBackedPlacementHistory("source-context", "GOAL", goal.id) } returns true
        coEvery { structures.getStructureByContext("source-context") } returns null
        coEvery {
            placements.setContextBackedPlacementVisible(
                contextId = "source-context",
                itemType = "GOAL",
                entityId = goal.id,
                visible = true,
                now = any(),
            )
        } returns "stable-placement"

        repository(
            goalDao = goalDao,
            associations = associations,
            placements = placements,
            structures = structures,
        ).updateGoal(goal)

        coVerify(exactly = 1) { associations.syncGoalAssociations(any(), "source-context") }
        coVerify(exactly = 1) {
            placements.setContextBackedPlacementVisible(
                contextId = "source-context",
                itemType = "GOAL",
                entityId = goal.id,
                visible = true,
                now = any(),
            )
        }
    }

    private fun repository(
        goalDao: GoalDao,
        associations: TagAssociationHandler,
        placements: BacklogPlacementCommands,
        structures: ContextStructureRepository,
        access: SystemContextCanonicalBacklogConfigurationAccess = mockk(relaxed = true),
    ): GoalRepository =
        GoalRepository(
            goalDao = goalDao,
            reminderRepository = mockk(relaxed = true),
            contextMarkerHandlerProvider = mockk<Provider<ContextMarkerHandler>>(relaxed = true),
            contextDao = mockk<ContextDao>(relaxed = true),
            tagAssociationHandler = associations,
            systemWorkspaceTagAuthority = mockk<SystemWorkspaceTagAuthority>(relaxed = true),
            contextStructureRepository = structures,
            systemBacklogConfigurationAccess = access,
            backlogPlacementCommands = placements,
            database = mockk(relaxed = true),
            orientationDao = mockk(relaxed = true),
            canonicalOrientationRepository = mockk(relaxed = true),
            goalOrientationBridge = mockk<GoalOrientationBridge>(relaxed = true),
            systemWorkspacePresentationContextProjector = mockk(relaxed = true),
        )
}
