package com.romankozak.forwardappmobile.domain.tags

import org.junit.Assert.assertEquals
import org.junit.Test

class HashTagCatalogTest {
    @Test
    fun combinesTagsFromTextsAndExplicitFields() {
        val tags =
            buildHashTagCatalog(
                texts = sequenceOf("Journal #health", "Backlog #Project", "Day task #focus"),
                explicitTags = sequenceOf("work", "#project", "invalid tag"),
            )

        assertEquals(listOf("#focus", "#health", "#Project", "#work"), tags)
    }
}
