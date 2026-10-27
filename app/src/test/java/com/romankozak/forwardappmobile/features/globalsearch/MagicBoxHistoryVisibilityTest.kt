package com.romankozak.forwardappmobile.features.globalsearch

import org.junit.Assert.*
import org.junit.Test

class MagicBoxHistoryVisibilityTest {
    private val history = listOf("health", "saved:Home")

    @Test fun `empty input exposes saved history`() {
        assertTrue(shouldShowSearchHistoryHint(OmniboxMode.DataSearch, "", history))
        assertTrue(shouldShowSearchHistoryHint(OmniboxMode.DataSearch, "  ", history))
    }

    @Test fun `typing hides history without waiting for submit or results`() {
        listOf("h", "health", "#health", "type:orientation", "saved:Home").forEach {
            assertFalse(shouldShowSearchHistoryHint(OmniboxMode.DataSearch, it, history))
        }
    }

    @Test fun `clearing query restores history without deleting it`() {
        assertFalse(shouldShowSearchHistoryHint(OmniboxMode.DataSearch, "health", history))
        assertTrue(shouldShowSearchHistoryHint(OmniboxMode.DataSearch, "", history))
        assertEquals(listOf("health", "saved:Home"), history)
    }

    @Test fun `other modes and empty history do not expose search history`() {
        OmniboxMode.entries.filter { it != OmniboxMode.DataSearch }.forEach {
            assertFalse(shouldShowSearchHistoryHint(it, "", history))
        }
        assertFalse(shouldShowSearchHistoryHint(OmniboxMode.DataSearch, "", emptyList()))
    }
}
