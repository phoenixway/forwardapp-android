package com.romankozak.forwardappmobile.domain.ner

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReminderParserCancellationTest {
    private var callback: ((List<Entity>?) -> Unit)? = null
    private val manager = mockk<NerManager> {
        every { nerState } returns MutableStateFlow<NerState>(NerState.Ready)
        every { predictAsync(any(), any()) } answers { callback = secondArg() }
    }
    private val parser = ReminderParser(manager)

    @Test fun `missing NER callback cannot extend timeout and late callback is harmless`() = runTest {
        val result = async { parser.parseWithTimeout("Ordinary goal", 100) }
        runCurrent()
        assertNotNull(callback)
        advanceTimeBy(100); runCurrent()
        assertTrue(result.isCompleted)
        assertFalse(result.await().success)
        callback!!(null)
    }

    @Test fun `timeout still applies deterministic reminder fallback`() = runTest {
        val result = async { parser.parseWithTimeout("Подзвонити через 5 хв", 100) }
        runCurrent(); advanceTimeBy(100); runCurrent()
        assertTrue(result.isCompleted)
        assertTrue(result.await().success)
        assertNotNull(result.await().calendar)
        callback!!(null)
    }

    @Test fun `caller cancellation propagates instead of becoming a fallback result`() = runTest {
        val result = async { parser.parseWithTimeout("Ordinary goal", 10_000) }
        runCurrent()
        result.cancel(); runCurrent()
        assertTrue(result.isCancelled)
        callback!!(null)
    }
}
