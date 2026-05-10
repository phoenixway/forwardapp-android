package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalLogLineParserTest {
    @Test
    fun `parses symbolic marker`() {
        val parsed = parseJournalLine("!! критична ціль")

        assertEquals("!!", parsed?.marker)
        assertEquals("критична ціль", parsed?.text)
    }

    @Test
    fun `parses ascii word marker`() {
        val parsed = parseJournalLine("bla будь що")

        assertEquals("bla", parsed?.marker)
        assertEquals("будь що", parsed?.text)
    }

    @Test
    fun `does not treat single cyrillic letter as marker`() {
        val parsed = parseJournalLine("с лово")

        assertNull(parsed?.marker)
        assertEquals("с лово", parsed?.text)
    }

    @Test
    fun `parses single cyrillic letter marker from allowlist`() {
        val parsed = parseJournalLine("і роздільник")

        assertEquals("і", parsed?.marker)
        assertEquals("роздільник", parsed?.text)
    }

    @Test
    fun `parses single cyrillic t marker from allowlist`() {
        val parsed = parseJournalLine("т ціль тижня")

        assertEquals("т", parsed?.marker)
        assertEquals("ціль тижня", parsed?.text)
    }

    @Test
    fun `keeps plain line without marker`() {
        val parsed = parseJournalLine("звичайний запис")

        assertNull(parsed?.marker)
        assertEquals("звичайний запис", parsed?.text)
    }
}
