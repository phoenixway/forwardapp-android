package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DayPlanReadFirstTest {
    @Test fun `persisted day is emitted before preparation and metadata updates never restart it`() = runTest {
        val ready = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val source = MutableStateFlow<DayPlan?>(DayPlan(id = "d", date = 1))
        val emitted = mutableListOf<DayPlan?>(); var calls = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeDayPlanReadFirst(source, ready) { calls++; finish.await() }.collect { emitted += it }
        }
        runCurrent()
        assertThat(emitted.single()!!.id).isEqualTo("d"); assertThat(calls).isEqualTo(0)
        ready.complete(Unit); runCurrent()
        source.value = source.value!!.copy(name = "Renamed")
        runCurrent()
        assertThat(emitted.last()!!.name).isEqualTo("Renamed") // Does not wait for preparation.
        assertThat(calls).isEqualTo(1)
        finish.complete(Unit); runCurrent()
        source.value = source.value!!.copy(name = "Again")
        runCurrent(); assertThat(calls).isEqualTo(1)
    }

    @Test fun `leaving the day cancels preparation instead of detached background writes`() = runTest {
        val ready = CompletableDeferred<Unit>().also { it.complete(Unit) }
        val entered = CompletableDeferred<Unit>(); var cancelled = false
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeDayPlanReadFirst(MutableStateFlow(DayPlan(id = "d", date = 1)), ready) {
                entered.complete(Unit)
                try { awaitCancellation() } finally { cancelled = true }
            }.collect()
        }
        entered.await(); job.cancelAndJoin()
        assertThat(cancelled).isTrue()
    }

    @Test fun `actual rule changes prepare the open day again without requiring navigation`() = runTest {
        val ready = CompletableDeferred<Unit>().also { it.complete(Unit) }
        val rules = MutableSharedFlow<Unit>()
        var calls = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeDayPlanReadFirst(MutableStateFlow(DayPlan(id = "d", date = 1)), ready, rules) { calls++ }.collect()
        }
        runCurrent(); assertThat(calls).isEqualTo(1)
        rules.emit(Unit); runCurrent()
        assertThat(calls).isEqualTo(2)
    }

    @Test fun `preparation failure remains visible to the existing error handler`() = runTest {
        val ready = CompletableDeferred<Unit>().also { it.complete(Unit) }
        val result = runCatching {
            observeDayPlanReadFirst(flowOf(DayPlan(id = "d", date = 1)), ready) { error("Invalid recurrence") }.collect()
        }
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Invalid recurrence")
    }
}
