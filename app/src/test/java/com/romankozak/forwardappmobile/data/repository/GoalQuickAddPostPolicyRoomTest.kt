package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogConfigurationAccess
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class GoalQuickAddPostPolicyRoomTest {
    @Test fun `new Goal without hashtag copies does not enqueue a redundant visibility command`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val placements = mockk<BacklogPlacementCommands>(relaxed = true)
            coEvery { placements.addToContextBacked("w", "GOAL", any()) } returns "placement"
            val repository = repository(f, placements, emptyMap(), false)
            assertEquals("placement", repository.addGoalToContext("Plain goal", "w"))
            assertEquals(1, f.db.goalDao().getAllVisible().size)
            assertEquals(1, f.dao.getAllManagedSubjects().count { !it.isDeleted })
            coVerify(exactly = 0) { placements.setContextBackedPlacementVisible(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { placements.hasContextBackedPlacementHistory(any(), any(), any()) }
        }
    }

    @Test fun `new Goal with hashtag copies retains enabled auto-hide policy`() = runBlocking {
        verifyAssociatedCreation(hide = true, expectedVisible = false)
    }

    @Test fun `new Goal with hashtag copies retains disabled auto-hide policy`() = runBlocking {
        verifyAssociatedCreation(hide = false, expectedVisible = true)
    }

    private suspend fun verifyAssociatedCreation(hide: Boolean, expectedVisible: Boolean) {
        SemanticDeletionRoomFixture().use { f ->
            val placements = mockk<BacklogPlacementCommands>(relaxed = true)
            coEvery { placements.addToContextBacked("w", "GOAL", any()) } returns "placement"
            coEvery { placements.hasContextBackedPlacementHistory("w", "GOAL", any()) } returns true
            repository(f, placements, mapOf("associated" to "tag"), hide).addGoalToContext("#tag goal", "w")
            coVerify(exactly = 1) { placements.setContextBackedPlacementVisible("w", "GOAL", any(), expectedVisible, any()) }
        }
    }

    private fun repository(
        f: SemanticDeletionRoomFixture,
        placements: BacklogPlacementCommands,
        associated: Map<String, String>,
        hide: Boolean,
    ): GoalRepository {
        val tags = mockk<SystemWorkspaceTagAuthority>()
        coEvery { tags.operationalTags("w") } returns emptyList()
        val associations = mockk<TagAssociationHandler>()
        coEvery { associations.syncGoalAssociations(any(), "w") } returns associated
        val structures = mockk<ContextStructureRepository>()
        coEvery { structures.getStructureByContext("w") } returns
            com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration.default("w")
                .copy(removeBacklogEntryAfterTagAutocopy = hide)
        val systemConfig = mockk<SystemContextCanonicalBacklogConfigurationAccess>()
        coEvery { systemConfig.getState("w") } returns null
        return GoalRepository(
            goalDao = f.db.goalDao(), reminderRepository = mockk(relaxed = true),
            contextMarkerHandlerProvider = Provider { mockk<ContextMarkerHandler>(relaxed = true) },
            contextDao = f.db.contextDao(), tagAssociationHandler = associations, systemWorkspaceTagAuthority = tags,
            contextStructureRepository = structures, systemBacklogConfigurationAccess = systemConfig,
            backlogPlacementCommands = placements, database = f.db, orientationDao = f.dao,
            canonicalOrientationRepository = f.orientations, canonicalScoringAuthorityActivator = mockk(relaxed = true),
            goalOrientationBridge = mockk(relaxed = true), canonicalGoalSemanticReadRepository = mockk(relaxed = true),
            systemWorkspacePresentationContextProjector = mockk(relaxed = true),
        )
    }
}
