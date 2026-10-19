package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.GoalWithReminderOutcome
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InputHandlerGoalSubmissionTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful Goal write clears input and selects created Backlog item`() = runBlocking {
        val h = harness()
        try {
            coEvery {
                h.goals.addGoalToContext("Original goal", "workspace-1", false)
            } returns "backlog-1"

            h.start("Original goal")

            assertThat(withTimeout(10_000) { h.listener.added.await() })
                .isEqualTo("backlog-1")
            assertThat(h.listener.text.text).isEmpty()
            assertThat(h.failure.isCompleted).isFalse()
            coVerify(exactly = 1) {
                h.goals.addGoalToContext("Original goal", "workspace-1", false)
            }
        } finally {
            h.scope.cancel()
        }
    }

    @Test
    fun `failed Goal write preserves input and reports error`() = runBlocking {
        val h = harness()
        try {
            coEvery {
                h.goals.addGoalToContext("Keep this goal", "workspace-1", false)
            } throws IllegalStateException("Simulated persistence failure")

            h.start("Keep this goal")

            assertThat(withTimeout(10_000) { h.failure.await() })
                .contains("Текст збережено")
            assertThat(h.listener.text.text).isEqualTo("Keep this goal")
            assertThat(h.listener.added.isCompleted).isFalse()
            coVerify(exactly = 1) {
                h.goals.addGoalToContext("Keep this goal", "workspace-1", false)
            }
        } finally {
            h.scope.cancel()
        }
    }

    @Test
    fun `double submit while persistence is pending writes once`() = runBlocking {
        val h = harness()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<String>()
        try {
            coEvery {
                h.goals.addGoalToContext("One goal", "workspace-1", false)
            } coAnswers {
                entered.complete(Unit)
                release.await()
            }

            h.start("One goal")
            withTimeout(10_000) { entered.await() }

            h.handler.submitInput(h.listener.text, InputMode.AddGoal)

            coVerify(exactly = 1) {
                h.goals.addGoalToContext("One goal", "workspace-1", false)
            }

            release.complete("backlog-1")
            assertThat(withTimeout(10_000) { h.listener.added.await() })
                .isEqualTo("backlog-1")

            coVerify(exactly = 1) {
                h.goals.addGoalToContext("One goal", "workspace-1", false)
            }
        } finally {
            h.scope.cancel()
        }
    }

    @Test
    fun `late write completion never clears text edited during submission`() = runBlocking {
        val h = harness()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<String>()
        try {
            coEvery {
                h.goals.addGoalToContext("First draft", "workspace-1", false)
            } coAnswers {
                entered.complete(Unit)
                release.await()
            }

            h.start("First draft")
            withTimeout(10_000) { entered.await() }

            h.handler.onInputTextChanged(
                TextFieldValue("Second draft"),
                InputMode.AddGoal,
            )

            release.complete("backlog-first")
            assertThat(withTimeout(10_000) { h.listener.added.await() })
                .isEqualTo("backlog-first")

            assertThat(h.listener.text.text).isEqualTo("Second draft")
            coVerify(exactly = 1) {
                h.goals.addGoalToContext("First draft", "workspace-1", false)
            }
        } finally {
            h.scope.cancel()
        }
    }


    @Test
    fun `successful reminder Goal clears input without warning`() = runBlocking {
        val h = harness()
        val original = "Call dentist tomorrow"
        val reminderAt = Calendar.getInstance().apply {
            timeInMillis = 1_900_000_000_000L
        }
        val goal = Goal(
            id = "goal-reminder-success",
            text = "Call dentist",
            completed = false,
            createdAt = 1L,
            updatedAt = 1L,
        )
        try {
            coEvery { h.parser.parseWithTimeout(original, any()) } returns
                ReminderParseResult(
                    originalText = original,
                    dateTimeEntities = emptyList(),
                    otherEntities = emptyList(),
                    success = true,
                    calendar = reminderAt,
                    suggestionText = "tomorrow",
                )
            coEvery {
                h.goals.addGoalWithReminder(
                    "Call dentist",
                    "workspace-1",
                    reminderAt.timeInMillis,
                )
            } returns GoalWithReminderOutcome(goal, reminderSetupSucceeded = true)

            h.start(original)

            assertThat(withTimeout(10_000) { h.listener.added.await() })
                .isEqualTo(goal.id)
            assertThat(h.listener.text.text).isEmpty()
            assertThat(h.failure.isCompleted).isFalse()
            coVerify(exactly = 1) {
                h.goals.addGoalWithReminder(
                    "Call dentist",
                    "workspace-1",
                    reminderAt.timeInMillis,
                )
            }
        } finally {
            h.scope.cancel()
        }
    }

    @Test
    fun `reminder setup failure keeps created Goal and warns without retry`() = runBlocking {
        val h = harness()
        val original = "Call dentist tomorrow"
        val reminderAt = Calendar.getInstance().apply {
            timeInMillis = 1_900_000_000_000L
        }
        val goal = Goal(
            id = "goal-reminder-partial",
            text = "Call dentist",
            completed = false,
            createdAt = 1L,
            updatedAt = 1L,
        )
        try {
            coEvery { h.parser.parseWithTimeout(original, any()) } returns
                ReminderParseResult(
                    originalText = original,
                    dateTimeEntities = emptyList(),
                    otherEntities = emptyList(),
                    success = true,
                    calendar = reminderAt,
                    suggestionText = "tomorrow",
                )
            coEvery {
                h.goals.addGoalWithReminder(
                    "Call dentist",
                    "workspace-1",
                    reminderAt.timeInMillis,
                )
            } returns GoalWithReminderOutcome(goal, reminderSetupSucceeded = false)

            h.start(original)

            assertThat(withTimeout(10_000) { h.failure.await() })
                .contains("Ціль створено")
            assertThat(h.listener.added.await()).isEqualTo(goal.id)
            assertThat(h.listener.text.text).isEmpty()
            coVerify(exactly = 1) {
                h.goals.addGoalWithReminder(
                    "Call dentist",
                    "workspace-1",
                    reminderAt.timeInMillis,
                )
            }
        } finally {
            h.scope.cancel()
        }
    }

    private fun harness(): Harness {
        val goals = mockk<GoalRepository>()
        val parser = mockk<ReminderParser>()
        val listener = RecordingListener()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val workspace = MutableStateFlow("workspace-1")
        val failure = CompletableDeferred<String>()

        coEvery { parser.parseWithTimeout(any(), any()) } coAnswers {
            ReminderParseResult(
                originalText = firstArg(),
                dateTimeEntities = emptyList(),
                otherEntities = emptyList(),
                success = false,
            )
        }

        val handler =
            InputHandler(
                contextRepository = mockk<ContextRepository>(relaxed = true),
                goalRepository = goals,
                scope = scope,
                projectIdFlow = workspace,
                resultListener = listener,
                reminderParser = parser,
                onGoalSubmitError = { message -> failure.complete(message) },
            )
        return Harness(handler, goals, parser, listener, scope, failure)
    }

    private data class Harness(
        val handler: InputHandler,
        val goals: GoalRepository,
        val parser: ReminderParser,
        val listener: RecordingListener,
        val scope: CoroutineScope,
        val failure: CompletableDeferred<String>,
    ) {
        fun start(value: String) {
            handler.onInputTextChanged(TextFieldValue(value), InputMode.AddGoal)
            handler.submitInput(listener.text, InputMode.AddGoal)
        }
    }

    private class RecordingListener : InputHandler.ResultListener {
        var text = TextFieldValue("")
        val added = CompletableDeferred<String>()

        override fun updateInputState(
            inputValue: TextFieldValue?,
            inputMode: InputMode?,
            localSearchQuery: String?,
            newlyAddedItemId: String?,
            detectedReminderSuggestion: String?,
            detectedReminderCalendar: Calendar?,
            clearDetectedReminder: Boolean,
        ) {
            if (inputValue != null) text = inputValue
            if (newlyAddedItemId != null) added.complete(newlyAddedItemId)
        }

        override fun updateDialogState(
            showAddWebLinkDialog: Boolean?,
            showAddObsidianLinkDialog: Boolean?,
        ) = Unit

        override fun showRecentListsSheet(show: Boolean) = Unit

        override fun setPendingAction(
            actionType: GoalActionType,
            itemIds: Set<String>,
            goalIds: Set<String>,
        ) = Unit

        override fun requestNavigation(route: String) = Unit

        override fun forceRefresh() = Unit

        override fun addQuickRecord(text: String) = Unit

        override fun addIssue(text: String) = Unit

        override fun addProjectComment(text: String) = Unit

        override fun addMilestone(text: String) = Unit

        override fun addDirectionItem(text: String) = Unit

        override fun createObsidianNote(
            noteName: String,
            vault: String?,
        ) = Unit
    }
}
