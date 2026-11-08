package com.romankozak.forwardappmobile.domain.ner

import io.mockk.every
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderPlainTextFastPathTest {
    @Test fun `plain text completes without reading NER state or waiting for inference`() = runTest {
        val manager = mockk<NerManager>()
        val parser = ReminderParser(manager)
        listOf("Перевірити документи", "Купити корм котам", "Обробити лапу", "Call dentist", "", "  ").forEach { text ->
            val result = parser.parseWithTimeout(text)
            assertFalse(result.success)
            assertEquals(text, result.originalText)
            assertNull(result.calendar)
        }
        assertEquals(0L, testScheduler.currentTime)
        verify { manager wasNot Called }
    }

    @Test fun `temporal candidate still reaches NER and preserves its result`() = runTest {
        val text = "Подзвонити завтра"
        val manager = mockk<NerManager> {
            every { nerState } returns MutableStateFlow<NerState>(NerState.Ready)
            every { predictAsync(text, any()) } answers {
                secondArg<(List<Entity>?) -> Unit>()(listOf(Entity(label = "DATE", start = 11, end = text.length, text = "завтра")))
            }
        }
        val result = ReminderParser(manager).parseWithTimeout(text)
        assertTrue(result.success)
        assertNotNull(result.calendar)
        assertEquals("завтра", result.suggestionText)
        verify(exactly = 1) { manager.predictAsync(text, any()) }
    }

    @Test fun `all currently accepted NER dates survive candidate preflight`() {
        assertSupportedCandidates("DATE", listOf("сьогодні", "завтра", "післязавтра", "понеділок", "вівторок",
            "середа", "четвер", "п'ятниця", "субота", "неділя", "15.12", "15/12"))
    }

    @Test fun `all currently accepted NER times survive candidate preflight`() {
        assertSupportedCandidates("TIME", listOf("9", "18:30", "ранку", "вранці", "вдень", "обід",
            "вечора", "ввечері", "ночі", "вночі"))
    }

    @Test fun `all currently accepted NER duration units and number words survive candidate preflight`() {
        val units = listOf("хвилину", "хвилини", "хвилин", "хв", "годину", "години", "годин", "год",
            "днів", "дні", "день", "дн", "тижнів", "тижні", "тиждень", "тижн", "місяців", "місяці", "місяць",
            "років", "роки", "року", "рок")
        assertSupportedCandidates("DURATION", NerReminderParser.numberMap.keys.flatMap { number -> units.map { "через $number $it" } })
    }

    @Test fun `fallback and conservative candidates keep parsing regardless of case`() {
        listOf("ЗАВТРА", "через п'ять хв", "за рік", "о 12.30", "15.12", "Friday", "tomorrow", "в п’ятницю")
            .forEach { assertTrue(it, mayContainReminderExpression(it)) }
    }

    private fun assertSupportedCandidates(label: String, fragments: List<String>) {
        fragments.forEach { fragment ->
            val text = "Дія $fragment"
            val result = NerReminderParser.parse(text, listOf(Entity(label = label, start = 4, end = text.length, text = fragment)))
            assertTrue("Fixture must actually parse: $label $fragment", result.success)
            assertTrue("Preflight must not drop accepted $label $fragment", mayContainReminderExpression(text))
        }
    }
}
