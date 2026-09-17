package com.romankozak.forwardappmobile.data.logic

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTagLookup
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.data.repository.BacklogPlacementCommands
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogGoalAssociationLinkDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextTagRefDao
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
            val contextTagRefDao = mockk<ContextTagRefDao>()
            val contextDao = mockk<ContextDao>(relaxed = true)
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
            coEvery { contextTagRefDao.findContextsByTags(listOf("systemtag")) } returns emptyList()
            coEvery {
                authority.findCanonicalSystemOwnersByTags(listOf("systemtag"))
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
                    contextTagRefDao = contextTagRefDao,
                    contextDao = contextDao,
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

            coVerify(exactly = 0) { contextDao.getContextById(systemId) }
        }

    @Test
    fun `Goal hashtag routing preserves ordinary Context tag index behavior`() =
        runTest {
            val contextTagRefDao = mockk<ContextTagRefDao>()
            val contextDao = mockk<ContextDao>(relaxed = true)
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
                contextTagRefDao.findContextsByTags(listOf("ordinary"))
            } returns
                listOf(
                    ContextTagLookup(
                        contextId = "ordinary-context",
                        normalizedTag = "ordinary",
                    ),
                )
            coEvery {
                authority.findCanonicalSystemOwnersByTags(listOf("ordinary"))
            } returns emptyList()

            val inserted = slot<List<BacklogGoalAssociationLink>>()
            coEvery { associationDao.insertAll(capture(inserted)) } returns Unit

            val handler =
                TagAssociationHandler(
                    contextTagRefDao = contextTagRefDao,
                    contextDao = contextDao,
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

            assertThat(result).containsEntry("ordinary-context", "ordinary")
            assertThat(inserted.captured.single().contextId)
                .isEqualTo("ordinary-context")
        }

    @Test
    fun `Inbox hashtag cache accepts canonical System owner without System Context shell`() =
        runTest {
            val contextDao = mockk<ContextDao>()
            val inboxRecordDao = mockk<InboxRecordDao>(relaxed = true)
            val linkDao = mockk<InboxRecordLinkDao>()
            val authority = mockk<SystemWorkspaceTagAuthority>()
            val systemId = SystemContexts.INBOX.raw

            coEvery { contextDao.getAll() } returns emptyList()
            coEvery {
                authority.effectiveOwners(emptyList())
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
                    contextDao = contextDao,
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
