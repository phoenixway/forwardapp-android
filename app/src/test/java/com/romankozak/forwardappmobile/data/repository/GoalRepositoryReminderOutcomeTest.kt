package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoalRepositoryReminderOutcomeTest {
    @Test
    fun `successful reminder setup returns persisted Goal outcome`() = runTest {
        val reminders = mockk<ReminderRepository>()
        val repository = spyk(repository(reminders))
        val reminderTime = 1_900_000_000_000L

        coEvery {
            repository.addGoalToContext(any<Goal>(), "workspace-1")
        } returns "backlog-1"

        coEvery {
            reminders.createReminder(any(), "GOAL", reminderTime)
        } returns Unit

        val outcome =
            repository.addGoalWithReminder(
                "Reminder goal",
                "workspace-1",
                reminderTime,
            )

        assertThat(outcome.reminderSetupSucceeded).isTrue()
        assertThat(outcome.goal.text).isEqualTo("Reminder goal")

        coVerify(exactly = 1) {
            repository.addGoalToContext(any<Goal>(), "workspace-1")
        }
        coVerify(exactly = 1) {
            reminders.createReminder(outcome.goal.id, "GOAL", reminderTime)
        }
    }

    @Test
    fun `scheduler failure returns partial success without recreating Goal`() = runTest {
        val reminders = mockk<ReminderRepository>()
        val repository = spyk(repository(reminders))
        val reminderTime = 1_900_000_000_000L

        coEvery {
            repository.addGoalToContext(any<Goal>(), "workspace-1")
        } returns "backlog-1"

        coEvery {
            reminders.createReminder(any(), "GOAL", reminderTime)
        } throws IllegalStateException("Simulated alarm failure")

        val outcome =
            repository.addGoalWithReminder(
                "Persisted without alarm",
                "workspace-1",
                reminderTime,
            )

        assertThat(outcome.reminderSetupSucceeded).isFalse()
        assertThat(outcome.goal.text).isEqualTo("Persisted without alarm")
        assertThat(outcome.goal.id).isNotEmpty()

        coVerify(exactly = 1) {
            repository.addGoalToContext(any<Goal>(), "workspace-1")
        }
        coVerify(exactly = 1) {
            reminders.createReminder(outcome.goal.id, "GOAL", reminderTime)
        }
    }

    private fun repository(reminders: ReminderRepository): GoalRepository =
        GoalRepository(
            goalDao = mockk(relaxed = true),
            reminderRepository = reminders,
            contextMarkerHandlerProvider = mockk(relaxed = true),
            contextDao = mockk(relaxed = true),
            tagAssociationHandler = mockk(relaxed = true),
            systemWorkspaceTagAuthority = mockk(relaxed = true),
            contextStructureRepository = mockk(relaxed = true),
            systemBacklogConfigurationAccess = mockk(relaxed = true),
            backlogPlacementCommands = mockk(relaxed = true),
            database = mockk(relaxed = true),
            orientationDao = mockk(relaxed = true),
            canonicalOrientationRepository = mockk(relaxed = true),
            canonicalScoringAuthorityActivator = mockk(relaxed = true),
            goalOrientationBridge = mockk(relaxed = true),
            canonicalGoalSemanticReadRepository = mockk(relaxed = true),
            systemWorkspacePresentationContextProjector = mockk(relaxed = true),
        )
}
