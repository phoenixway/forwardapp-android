package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InboxSortingServiceTest {
    private val listItemRepository = mockk<ListItemRepository>()
    private val backlogPlacementCommands = mockk<BacklogPlacementCommands>()
    private val inboxRepository = mockk<InboxRepository>()
    private val contextRepository = mockk<ContextRepository>()
    private val connectionsRepository = mockk<CanonicalConnectionsRepository>()
    private val backlogRepository = mockk<CanonicalBacklogRepository>()
    private val canonicalInboxRepository = mockk<CanonicalInboxRepository>()
    private val sortingRepository = mockk<CanonicalInboxSortingRepository>()
    private val service =
        InboxSortingService(
            listItemRepository = listItemRepository,
            backlogPlacementCommands = backlogPlacementCommands,
            inboxRepository = inboxRepository,
            contextRepository = contextRepository,
            canonicalConnectionsRepository = connectionsRepository,
            canonicalBacklogRepository = backlogRepository,
            canonicalInboxRepository = canonicalInboxRepository,
            canonicalSortingRepository = sortingRepository,
        )

    @Test
    fun `apply validates policy and selected target even when target is empty`() = runBlocking {
        coEvery { sortingRepository.requireActive("owner") } returns Unit
        coEvery { backlogRepository.requireActive("owner") } returns Unit
        coEvery { listItemRepository.getBacklogItemsForContext("owner") } returns emptyList()

        assertEquals(
            0,
            service.applySorting(
                contextId = "owner",
                rulesText = "backlog:oldest",
                target = InboxSortingService.SortTarget.BACKLOG,
            ),
        )

        coVerify(exactly = 1) { sortingRepository.requireActive("owner") }
        coVerify(exactly = 1) { backlogRepository.requireActive("owner") }
        coVerify(exactly = 1) { listItemRepository.getBacklogItemsForContext("owner") }
    }
}
