package com.romankozak.forwardappmobile.data.logic

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.data.repository.BacklogPlacementCommands
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogGoalAssociationLinkDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SystemTagRuntimeRoutingTest {
    @Test
    fun `Goal hashtag routing uses canonical System owner without tag lookup through Context rows`() =
        runTest {
            val placements = mockk<BacklogPlacementCommands>()
            val associationDao = mockk<BacklogGoalAssociationLinkDao>()
            val inboxCache = mockk<InboxAssociationCache>(relaxed = true)
            val goalDao = mockk<GoalDao>(relaxed = true)
            val inboxRecordDao = mockk<InboxRecordDao>(relaxed = true)
            val authority = mockk<SystemWorkspaceTagAuthority>()

            val goal =
                Goal(
                    id = "goal",
                    text = "route #systemtag",
                    completed = false,
                    createdAt = 1L,
                    updatedAt = 2L,
                )
            val systemId = SystemContexts.INBOX.raw

            coEvery {
                placements.findLiveGoalWorkspaceIdsIfCutOver(goal.id)
            } returns emptyList()
            coEvery { associationDao.getLinksForGoal(goal.id) } returns emptyList()
            coEvery {
                authority.findOperationalOwnersByTags(listOf("systemtag"))
            } returns
                listOf(
                    SystemWorkspaceTagAuthority.TagMatch(
                        contextId = systemId,
                        normalizedTag = "systemtag",
                    ),
                )

            val inserted = slot<List<BacklogGoalAssociationLink>>()
            coEvery { associationDao.insertAll(capture(inserted)) } returns Unit

            val handler =
                TagAssociationHandler(
                    backlogPlacementCommands = placements,
                    backlogGoalAssociationLinkDao = associationDao,
                    inboxAssociationCache = inboxCache,
                    goalDao = goalDao,
                    inboxRecordDao = inboxRecordDao,
                    systemWorkspaceTagAuthority = authority,
                )

            val result =
                handler.syncGoalAssociations(
                    goal = goal,
                    sourceContextId = "source-owner",
                )

            assertThat(result).containsEntry(systemId, "systemtag")
            assertThat(inserted.captured.single().contextId).isEqualTo(systemId)
            assertThat(inserted.captured.single().associationTag).isEqualTo("systemtag")
        }

    @Test
    fun `Goal hashtag routing uses ordinary canonical Workspace tag owner`() =
        runTest {
            val placements = mockk<BacklogPlacementCommands>()
            val associationDao = mockk<BacklogGoalAssociationLinkDao>()
            val authority = mockk<SystemWorkspaceTagAuthority>()

            val goal =
                Goal(
                    id = "goal",
                    text = "route #ordinary",
                    completed = false,
                    createdAt = 1L,
                    updatedAt = 2L,
                )

            coEvery {
                placements.findLiveGoalWorkspaceIdsIfCutOver(goal.id)
            } returns emptyList()
            coEvery { associationDao.getLinksForGoal(goal.id) } returns emptyList()
            coEvery {
                authority.findOperationalOwnersByTags(listOf("ordinary"))
            } returns
                listOf(
                    SystemWorkspaceTagAuthority.TagMatch(
                        contextId = "ordinary-workspace",
                        normalizedTag = "ordinary",
                    ),
                )

            val inserted = slot<List<BacklogGoalAssociationLink>>()
            coEvery { associationDao.insertAll(capture(inserted)) } returns Unit

            val handler =
                TagAssociationHandler(
                    backlogPlacementCommands = placements,
                    backlogGoalAssociationLinkDao = associationDao,
                    inboxAssociationCache = mockk(relaxed = true),
                    goalDao = mockk(relaxed = true),
                    inboxRecordDao = mockk(relaxed = true),
                    systemWorkspaceTagAuthority = authority,
                )

            val result =
                handler.syncGoalAssociations(
                    goal = goal,
                    sourceContextId = "source-owner",
                )

            assertThat(result).containsEntry("ordinary-workspace", "ordinary")
            assertThat(inserted.captured.single().contextId)
                .isEqualTo("ordinary-workspace")
        }

    @Test
    fun `Inbox hashtag cache accepts canonical System owner without System Context shell`() =
        runTest {
            val inboxRecordDao = mockk<InboxRecordDao>(relaxed = true)
            val linkDao = mockk<InboxRecordLinkDao>()
            val authority = mockk<SystemWorkspaceTagAuthority>()
            val systemId = SystemContexts.INBOX.raw

            coEvery {
                authority.effectiveOwners()
            } returns
                listOf(
                    SystemWorkspaceTagAuthority.TagOwner(
                        id = systemId,
                        tags = listOf("canonical"),
                    ),
                )
            coEvery { linkDao.getLinksForRecord("record") } returns emptyList()
            coEvery { linkDao.insertAll(any()) } returns Unit

            val cache =
                InboxAssociationCache(
                    inboxRecordDao = inboxRecordDao,
                    inboxRecordLinkDao = linkDao,
                    systemWorkspaceTagAuthority = authority,
                )

            val result =
                cache.refresh(
                    InboxRecord(
                        id = "record",
                        contextId = "source-owner",
                        text = "hello #canonical",
                        createdAt = 1L,
                        order = 0L,
                    ),
                )

            assertThat(result).containsExactly(systemId, "canonical")
            coVerify {
                linkDao.insertAll(
                    match { links ->
                        links.size == 1 &&
                            links.single().contextId == systemId &&
                            links.single().associationTag == "canonical"
                    },
                )
            }
        }
}
