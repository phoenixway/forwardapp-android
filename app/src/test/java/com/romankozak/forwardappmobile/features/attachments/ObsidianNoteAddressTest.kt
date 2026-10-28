package com.romankozak.forwardappmobile.features.attachments

import org.junit.Assert.*
import org.junit.Test

class ObsidianNoteAddressTest {
    @Test fun `unique short note name uses file parameter`() {
        assertEquals("obsidian://open?vault=Vault&file=Health", ObsidianNoteAddress.parse(" Health ", "Vault").openUri(null))
    }
    @Test fun `vault relative path and optional md extension stay exact`() {
        listOf("Folder/Note", "Folder/Note.md").forEach { path ->
            assertEquals(path, ObsidianNoteAddress.parse(path, "V").target)
            assertEquals("obsidian://open?vault=V&file=${path.replace("/", "%2F")}", ObsidianNoteAddress.parse(path, "V").openUri(null))
        }
    }
    @Test fun `copied URL preserves encoded Unicode heading block and pane options`() {
        val url = "obsidian://open?vault=My%20Vault&file=%D0%9F%D0%BB%D0%B0%D0%BD%23%5Eblock&paneType=tab"
        val parsed = ObsidianNoteAddress.parse(url, "Ignored")
        assertEquals("My Vault", parsed.vault)
        assertEquals("План#^block", parsed.displayName)
        assertEquals(url, parsed.openUri("Ignored"))
    }
    @Test fun `absolute path URL requires no vault setting`() {
        val url = "obsidian://open?path=%2Fstorage%2FVault%2FNote.md"
        assertEquals(url, ObsidianNoteAddress.parse(url).openUri(null))
    }
    @Test fun `spaces plus ampersand and Unicode are encoded once`() {
        assertEquals("obsidian://open?vault=A%20%26%20B&file=%D0%9F%D0%BB%D0%B0%D0%BD%20%2B%20A",
            ObsidianNoteAddress.parse("План + A", "A & B").openUri(null))
    }
    @Test fun `bare names need explicit or fallback vault`() {
        assertNull(ObsidianNoteAddress.parse("Note").openUri(" "))
        assertEquals("obsidian://open?vault=Fallback&file=Note", ObsidianNoteAddress.parse("Note").openUri("Fallback"))
    }
    @Test fun `non-note URLs malformed encodings duplicates and traversal fail safely`() {
        listOf("", "https://example.org", "obsidian://new?vault=V&name=N", "obsidian://open?vault=V",
            "obsidian://open?vault=V&file=%ZZ", "obsidian://open?vault=V&vault=W&file=N", "../Note", "/Folder/Note").forEach {
            assertTrue(it, runCatching { ObsidianNoteAddress.parse(it) }.isFailure)
        }
    }
}
