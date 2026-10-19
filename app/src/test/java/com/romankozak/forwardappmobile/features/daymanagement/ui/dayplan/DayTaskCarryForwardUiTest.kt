package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DayTaskCarryForwardUiTest {
    private val repository = mockk<DayManagementRepository>()
    private val task = DayTask(id = "source", dayPlanId = "today", title = "Task")
    @Before fun setup() { mockkStatic(Log::class); every { Log.e(any(), any(), any()) } returns 0 }
    @After fun teardown() { unmockkStatic(Log::class) }

    @Test fun `commit confirmation does not wait for metrics`() = runTest {
        val metrics = CompletableDeferred<Unit>()
        coEvery { repository.moveTaskToTomorrow(task) } returns task.copy(id = "next", dayPlanId = "tomorrow")
        coEvery { repository.calculateAndSaveDailyMetrics("tomorrow") } coAnswers { metrics.await() }
        var cleared = false
        val messages = mutableListOf<String>()
        val job = launch { carryDayTaskToTomorrow(repository, DayTaskWithReminder(task, null), { cleared = true }, messages::add) }
        runCurrent()
        assertThat(cleared).isTrue()
        assertThat(messages.single()).contains("збережено")
        assertThat(job.isCompleted).isFalse()
        metrics.complete(Unit); job.join()
    }

    @Test fun `failed carry reports error without false success or metrics`() = runTest {
        coEvery { repository.moveTaskToTomorrow(task) } throws IllegalStateException("Participation already exists")
        var cleared = false
        val messages = mutableListOf<String>()
        carryDayTaskToTomorrow(repository, DayTaskWithReminder(task, null), { cleared = true }, messages::add)
        assertThat(cleared).isFalse()
        assertThat(messages.single()).contains("Не вдалося")
        coVerify(exactly = 0) { repository.calculateAndSaveDailyMetrics(any()) }
    }

    @Test fun `post commit metric failure does not undo or misreport committed carry`() = runTest {
        coEvery { repository.moveTaskToTomorrow(task) } returns task.copy(dayPlanId = "tomorrow")
        coEvery { repository.calculateAndSaveDailyMetrics(any()) } throws IllegalStateException("Metrics failed")
        val messages = mutableListOf<String>()
        carryDayTaskToTomorrow(repository, DayTaskWithReminder(task, null), {}, messages::add)
        assertThat(messages).hasSize(1)
        assertThat(messages.single()).doesNotContain("Не вдалося")
    }
}
