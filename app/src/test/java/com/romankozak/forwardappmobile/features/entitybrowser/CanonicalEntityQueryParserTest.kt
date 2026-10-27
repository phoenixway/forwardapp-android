package com.romankozak.forwardappmobile.features.entitybrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanonicalEntityQueryParserTest {
    @Test fun `multiple tags parse with canonical AND membership`() {
        val result = parseCanonicalEntityQuery("#Health #finance")
        assertThat(result.isValid).isTrue()
        assertThat(result.query.tags).containsExactly("health", "finance")
        assertThat(result.query.text).isEmpty()
    }
    @Test fun `type parser admits only the three canonical identity classes`() {
        for (type in CanonicalEntityClass.entries) {
            assertThat(parseCanonicalEntityQuery("type:${type.name.lowercase()}").query.entityTypes).containsExactly(type)
        }
        assertThat(parseCanonicalEntityQuery("type:context").isValid).isFalse()
        assertThat(parseCanonicalEntityQuery("type:mission").isValid).isFalse()
    }
    @Test fun `score minimum maximum and repeated bounds intersect`() {
        val query = parseCanonicalEntityQuery("score:importance>=4 score:importance>=7 score:importance<=10").query
        assertThat(query.scoreFilters[CanonicalScoreDimension.IMPORTANCE]).isEqualTo(CanonicalScoreRange(7.0, 10.0))
    }
    @Test fun `score sort uses typed dimension and explicit direction`() {
        assertThat(parseCanonicalEntityQuery("sort:importance").query.sort).isEqualTo(CanonicalEntitySort(CanonicalEntitySortKey.SCORE, dimension = CanonicalScoreDimension.IMPORTANCE))
        assertThat(parseCanonicalEntityQuery("sort:effort:desc").query.sort).isEqualTo(CanonicalEntitySort(CanonicalEntitySortKey.SCORE, CanonicalSortDirection.DESC, CanonicalScoreDimension.EFFORT))
    }
    @Test fun `free text and all operators compose`() {
        val result = parseCanonicalEntityQuery("лікування #health type:orientation score:importance>=7 sort:importance:desc is:unplaced")
        assertThat(result.isValid).isTrue()
        assertThat(result.query.text).isEqualTo("лікування")
        assertThat(result.query.unplaced).isTrue()
        assertThat(result.query.entityTypes).containsExactly(CanonicalEntityClass.ORIENTATION)
    }
    @Test fun `unknown invalid and conflicting predicates fail safely without broad results`() {
        for (text in listOf("#", "unknown:value", "score:urgency>=5", "score:importance>=NaN", "sort:unknown", "sort:risk:sideways",
            "sort:cost:asc:extra", "score:importance>=9 score:importance<=3")) {
            val result = parseCanonicalEntityQuery(text)
            assertThat(result.errors).isNotEmpty()
            assertThat(result.query.entityTypes).isEmpty()
        }
    }
    @Test fun `graphical and textual queries return exactly equivalent typed identities`() {
        val catalog = listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("same"), "Health", scores = mapOf(CanonicalScoreDimension.IMPORTANCE to 7.0)),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("same"), "Health", tags = setOf("health")),
        )
        val pairs = listOf(
            "#health type:workspace" to CanonicalEntityQuery(entityTypes = setOf(CanonicalEntityClass.WORKSPACE), tags = setOf("health")),
            "Health type:orientation score:importance>=7 sort:importance:desc" to CanonicalEntityQuery(text = "Health",
                entityTypes = setOf(CanonicalEntityClass.ORIENTATION), scoreFilters = mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(min = 7.0)),
                sort = CanonicalEntitySort(CanonicalEntitySortKey.SCORE, CanonicalSortDirection.DESC, CanonicalScoreDimension.IMPORTANCE)),
        )
        pairs.forEach { (text, graphical) ->
            val parsed = parseCanonicalEntityQuery(text)
            assertThat(parsed.isValid).isTrue()
            assertThat(executeCanonicalEntityQuery(catalog, parsed.query).map { it.ref }).isEqualTo(executeCanonicalEntityQuery(catalog, graphical).map { it.ref })
        }
    }
    @Test fun `multiple types form a union within the type predicate`() {
        assertThat(parseCanonicalEntityQuery("type:aspect type:workspace").query.entityTypes).containsExactly(CanonicalEntityClass.ASPECT, CanonicalEntityClass.WORKSPACE)
    }
}
