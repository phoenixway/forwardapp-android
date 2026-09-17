package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderActionsTest {
    @Test
    fun `project reminder setup accepts stable read fields without Context backing`() =
        runTest {
            val reminderRepository = mockk<ReminderRepository>()
            val projectId = SystemContexts.STRATEGIC.raw
            every { reminderRepository.getRemindersForEntityFlow(projectId) } returns flowOf(emptyList<Reminder>())
            val stateManager = ContextStateManager(this)
            val actions =
                ReminderActions(
                    reminderRepository = reminderRepository,
                    stateManager = stateManager,
                    uiState = stateManager.uiState,
                    showSnackbar = { _, _ -> },
                    forceRefresh = {},
                )

            actions.onSetReminderForProject(
                projectId = projectId,
                projectName = "Strategic",
                projectCreatedAt = 0L,
            )

            val record = requireNotNull(stateManager.uiState.value.recordForReminderDialog)
            assertEquals(projectId, record.id)
            assertEquals("Strategic", record.text)
            assertEquals(projectId, record.contextId)
        }
}
