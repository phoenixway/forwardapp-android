package com.romankozak.forwardappmobile.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class BacklogAddTraceRecorderTest {
    private var clock = 100L
    private val logs = mutableListOf<String>()
    private val recorder = BacklogAddTraceRecorder({ clock }, logs::add)

    @Test fun `monotonic elapsed and duration are correlated without any private payload`() {
        val operation = recorder.start("private-workspace")
        operation.goalId = "private-goal"
        operation.placementId = "private-placement"
        clock += 30
        operation.mark("write.committed", 12)
        assertEquals(listOf("op=1 +0ms submit", "op=1 +30ms write.committed duration=12ms"), logs)
        assertFalse(logs.any { it.contains("private") })
    }

    @Test fun `only exact target in the matching Workspace marks delivery`() {
        val first = recorder.start("one").apply { placementId = "a"; goalId = "goal-a" }
        recorder.start("two").apply { placementId = "b"; goalId = "goal-b" }
        recorder.targetObserved("two", "observer.target_received") { placement, _ -> placement == "a" }
        assertEquals(2, logs.size)
        recorder.targetObserved("one", "observer.target_received") { _, goal -> goal == "goal-a" }
        assertEquals("op=1 +0ms observer.target_received", logs.last())
        assertFalse(first.terminal)
    }

    @Test fun `visible layout and input update are distinct and completion waits for both`() {
        val operation = recorder.start("one").apply { placementId = "placement" }
        operation.mark("input.updated")
        assertFalse(operation.terminal)
        recorder.targetObserved(null, "ui.visible") { placement, _ -> placement == "placement" }
        assertTrue(operation.terminal)
        assertEquals("op=1 +0ms complete", logs.last())
        val count = logs.size
        operation.mark("ui.visible")
        assertEquals(count, logs.size)
    }

    @Test fun `layout may arrive before command returns without prematurely completing trace`() {
        val operation = recorder.start("one")
        operation.mark("ui.visible")
        assertFalse(operation.terminal)
        operation.mark("repository.returned")
        clock += 20
        operation.mark("input.updated")
        assertTrue(operation.terminal)
        assertEquals("op=1 +20ms complete", logs.last())
    }

    @Test fun `failed or cancelled operation stops and cannot be matched to later rows`() {
        listOf("failed", "cancelled").forEach { stage ->
            val operation = recorder.start("one").apply { goalId = "goal" }
            operation.mark(stage)
            val count = logs.size
            recorder.targetObserved("one", "ui.visible") { _, _ -> true }
            assertEquals(count, logs.size)
            assertTrue(operation.terminal)
        }
    }

    @Test fun `retention is bounded if user leaves screen or operation never gets layout`() {
        repeat(40) { recorder.start("one") }
        assertEquals(32, recorder.pending("one").size)
        clock += 60_001
        assertTrue(recorder.pending("one").isEmpty())
    }

    @Test fun `repeated read spans remain visible while duplicate target notifications are suppressed`() {
        val operation = recorder.start("one").apply { goalId = "goal" }
        repeat(2) { operation.mark("backlog.snapshot.begin", once = false) }
        repeat(2) { recorder.targetObserved("one", "observer.target_received") { _, _ -> true } }
        assertEquals(2, logs.count { it.endsWith("backlog.snapshot.begin") })
        assertEquals(1, logs.count { it.endsWith("observer.target_received") })
    }

    @Test fun `correlation survives dispatcher hop used by repository transactions`() = runTest {
        val operation = recorder.start("one")
        withContext(operation) {
            withContext(Dispatchers.Default) {
                assertSame(operation, currentCoroutineContext()[BacklogAddTraceRecorder.Operation])
            }
        }
    }
}
