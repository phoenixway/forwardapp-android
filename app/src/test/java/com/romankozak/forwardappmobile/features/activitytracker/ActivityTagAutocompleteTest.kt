package com.romankozak.forwardappmobile.features.activitytracker

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityTagAutocompleteTest {
    @Test
    fun suggestionsMatchActiveHashTagAtEndOfInput() {
        val suggestions =
            buildActivityTagSuggestions(
                inputText = "Працюю над #pro",
                knownTags = listOf("#project", "productivity", "#rest"),
            )

        assertEquals(listOf("#productivity", "#project"), suggestions)
    }

    @Test
    fun enteringHashShowsKnownTags() {
        val suggestions =
            buildActivityTagSuggestions(
                inputText = "Починаю #",
                knownTags = listOf("work", "#rest"),
            )

        assertEquals(listOf("#rest", "#work"), suggestions)
    }

    @Test
    fun selectingSuggestionReplacesOnlyActiveFragment() {
        val result = applyActivityTagSuggestion("Ранок #health, робота #pro", "#project")

        assertEquals("Ранок #health, робота #project ", result)
    }

    @Test
    fun suggestionsStayHiddenWithoutActiveHashTag() {
        val suggestions = buildActivityTagSuggestions("Завершив #work ", listOf("#work"))

        assertEquals(emptyList<String>(), suggestions)
    }
}
