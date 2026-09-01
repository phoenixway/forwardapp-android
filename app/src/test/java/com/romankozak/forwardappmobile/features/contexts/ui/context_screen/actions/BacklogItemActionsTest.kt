package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextTimeTrackingRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BacklogItemActionsTest {
    @Test
    fun `LinkItem delete uses domain id and canonical lifecycle boundary`() = runTest {
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        val listItemRepository = mockk<ListItemRepository>(relaxed = true)

        val actions =
            BacklogItemActions(
                BacklogItemRepositories(
                    goalRepository = mockk<GoalRepository>(relaxed = true),
                    contextRepository = contextRepository,
                    noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                    musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                    checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                    noteRepository = mockk<LegacyNoteRepository>(relaxed = true),
                    listItemRepository = listItemRepository,
                    dayManagementRepository = mockk<DayManagementRepository>(relaxed = true),
                    activityRepository = mockk<ActivityRepository>(relaxed = true),
                    contextTimeTrackingRepository = mockk<ContextTimeTrackingRepository>(relaxed = true),
                ),
            )

        val link = mockk<LinkItemEntity>()
        every { link.id } returns "link-domain-1"

        val item = mockk<BacklogItemContent.LinkItem>()
        every { item.link } returns link

        val result = actions.deleteEverywhere(item)

        assertEquals("Посилання видалено", result)

        coVerify(exactly = 1) {
            contextRepository.deleteLinkItemEverywhere("link-domain-1")
        }
        coVerify(exactly = 0) {
            contextRepository.deleteAttachmentEverywhere(any())
        }
    }
}
