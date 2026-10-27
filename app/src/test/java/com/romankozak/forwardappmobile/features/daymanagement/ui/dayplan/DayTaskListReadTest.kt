package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.data.planning.day.DayTaskGoalParent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class DayTaskListReadTest {
    @Test fun `committed removal appears while all metadata streams are still suspended`() = runBlocking {
        val rows = MutableStateFlow(listOf(DayTask(id = "task", dayPlanId = "day", title = "Task")))
        val never = CompletableDeferred<Unit>()
        val reminders = flow<List<Reminder>> { never.await() }
        val parents = flow<Map<String, DayTaskGoalParent>> { never.await() }
        val read = observeDayTaskList(rows, reminders, parents, flow { never.await(); emit(emptyList()) })
        assertThat(withTimeout(5_000) { read.first() }.single().dayTask.id).isEqualTo("task")
        val initial = CompletableDeferred<Unit>()
        val deleted = async { withTimeout(5_000) { read.onEach { initial.complete(Unit) }.first { it.isEmpty() } } }
        initial.await()
        rows.value = emptyList() // Only committed DAO emission removes the row.
        assertThat(deleted.await()).isEmpty()
    }

    @Test fun `task changes do not recreate bulk metadata subscriptions and canonical title updates react`() = runBlocking {
        val subscriptions = AtomicInteger()
        val bothSubscribed = CompletableDeferred<Unit>()
        fun subscribed() { if (subscriptions.incrementAndGet() == 2) bothSubscribed.complete(Unit) }
        val parents = MutableStateFlow(mapOf("goal" to DayTaskGoalParent("goal", "Canonical", "workspace")))
        val reminders = MutableStateFlow(emptyList<Reminder>())
        val rows = MutableStateFlow(listOf(DayTask(id = "task", dayPlanId = "day", title = "Task", goalId = "goal")))
        val read = observeDayTaskList(rows, reminders.onStart { subscribed() }, parents.onStart { subscribed() }, flowOf(emptyList()))
        val started = CompletableDeferred<Unit>()
        val renamed = async { withTimeout(5_000) { read.onEach { if (it.firstOrNull()?.parentInfo?.title == "Canonical") started.complete(Unit) }
            .first { it.firstOrNull()?.parentInfo?.title == "Updated" } } }
        started.await()
        withTimeout(5_000) { bothSubscribed.await() }
        rows.value = rows.value + DayTask(id = "other", dayPlanId = "day", title = "Other")
        parents.value = mapOf("goal" to DayTaskGoalParent("goal", "Updated", "workspace"))
        val result = renamed.await()
        assertThat(result.first().parentInfo?.projectId).isEqualTo("workspace")
        assertThat(subscriptions.get()).isEqualTo(2)
    }
}
