package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DayFocusLinkRemovalTest {
    private val link = RelatedLink(LinkType.CONTEXT, "old", "Old Context")

    @Test fun `draft changes only after persistence and double taps cannot dispatch twice`() = runTest {
        val state = DayFocusLinkRemoval()
        val pending = CompletableDeferred<Unit>()
        var writes = 0
        var removals = 0
        val persist: suspend (RelatedLink) -> Unit = { writes++; pending.await() }
        val job = launch { state.remove(link, persist) { removals++ } }
        runCurrent()
        assertTrue(state.busy)
        assertEquals(0, removals)
        state.remove(link, persist) { removals++ }
        assertEquals(1, writes)
        pending.complete(Unit)
        job.join()
        assertEquals(1, removals)
        assertFalse(state.busy)
        assertNull(state.error)
    }

    @Test fun `failed persistence keeps row visible and exposes retryable error`() = runTest {
        val state = DayFocusLinkRemoval()
        var removals = 0
        state.remove(link, { error("Write failed") }) { removals++ }
        assertEquals(0, removals)
        assertEquals("Write failed", state.error)
        assertFalse(state.busy)
        state.remove(link, {}) { removals++ }
        assertEquals(1, removals)
        assertNull(state.error)
    }

    @Test fun `cancellation keeps draft unchanged and clears busy without swallowing cancellation`() = runTest {
        val state = DayFocusLinkRemoval()
        var removals = 0
        val job = launch { state.remove(link, { CompletableDeferred<Unit>().await() }) { removals++ } }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse(state.busy)
        assertNull(state.error)
        assertEquals(0, removals)
    }

    @Test fun `new unsaved Focus removes only its local draft link`() = runTest {
        val state = DayFocusLinkRemoval()
        var removed: RelatedLink? = null
        state.remove(link, null) { removed = it }
        assertEquals(link, removed)
        assertFalse(state.busy)
    }
}
