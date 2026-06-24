package com.romankozak.forwardappmobile.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredSearchQueryTest {
    @Test
    fun `multiple tags use AND semantics with partial matching`() {
        val query = StructuredSearchQuery.parse("#work #safe")

        assertTrue(query.matches(listOf("Plan #work-project and #safety")))
        assertFalse(query.matches(listOf("Plan #work-project only")))
    }

    @Test
    fun `mixed query requires tags and text`() {
        val query = StructuredSearchQuery.parse("#work weekly report")

        assertTrue(query.matches(listOf("Weekly report for #work-project")))
        assertFalse(query.matches(listOf("Daily report for #work-project")))
    }

    @Test
    fun `parser removes tags from text query`() {
        val query = StructuredSearchQuery.parse("#work  weekly report #safe")

        assertEquals(listOf("work", "safe"), query.tags)
        assertEquals("weekly report", query.textQuery)
    }

    @Test
    fun `markdown heading is not treated as hashtag`() {
        assertEquals(emptyList<String>(), StructuredSearchQuery.extractHashtags("##Heading"))
        assertEquals(emptyList<String>(), StructuredSearchQuery.extractHashtags("# Heading"))
    }
}
