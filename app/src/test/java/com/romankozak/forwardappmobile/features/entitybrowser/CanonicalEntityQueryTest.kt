package com.romankozak.forwardappmobile.features.entitybrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class CanonicalEntityQueryTest {
    private val importance = CanonicalScoreDimension.IMPORTANCE
    private val impact = CanonicalScoreDimension.IMPACT
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Personal Health",
        scores = mapOf(importance to 7.0, impact to 5.0))
    private val higher = CanonicalEntitySummary(CanonicalEntityRef.Orientation("high"), "Health Plan",
        scores = mapOf(importance to 10.0, impact to 8.0))
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Health Workspace", tags = setOf("health", "finance"))
    private val other = CanonicalEntitySummary(CanonicalEntityRef.Workspace("other"), "Other", tags = setOf("health"))
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Health")
    private val catalog = listOf(orientation, higher, workspace, other, aspect)
    private fun query(q: CanonicalEntityQuery, placed: Set<CanonicalEntityRef> = emptySet()) = executeCanonicalEntityQuery(catalog, q, placed).map { it.ref }

    @Test fun `text reuses fuzzy matching`() {
        assertThat(query(CanonicalEntityQuery(text = "persnal helth"))).containsExactly(orientation.ref)
    }
    @Test fun `type predicate accepts only selected canonical class`() {
        assertThat(query(CanonicalEntityQuery(entityTypes = setOf(CanonicalEntityClass.ASPECT)))).containsExactly(aspect.ref)
    }
    @Test fun `single tag uses canonical normalized membership`() {
        assertThat(query(CanonicalEntityQuery(tags = setOf(" #HEALTH ")))).containsExactly(workspace.ref, other.ref)
    }
    @Test fun `multiple tags use AND not OR`() {
        assertThat(query(CanonicalEntityQuery(tags = setOf("health", "finance")))).containsExactly(workspace.ref)
    }
    @Test fun `text and tags compose`() {
        assertThat(query(CanonicalEntityQuery(text = "workspce", tags = setOf("health")))).containsExactly(workspace.ref)
    }
    @Test fun `type and tags compose without inheriting tags onto semantics`() {
        assertThat(query(CanonicalEntityQuery(entityTypes = setOf(CanonicalEntityClass.ORIENTATION), tags = setOf("health")))).isEmpty()
    }
    @Test fun `score minimum is inclusive`() {
        assertThat(query(CanonicalEntityQuery(scoreFilters = mapOf(importance to CanonicalScoreRange(min = 7.0))))).containsExactly(orientation.ref, higher.ref)
    }
    @Test fun `score maximum and bounded range are inclusive`() {
        assertThat(query(CanonicalEntityQuery(scoreFilters = mapOf(importance to CanonicalScoreRange(max = 7.0))))).containsExactly(orientation.ref)
        assertThat(query(CanonicalEntityQuery(scoreFilters = mapOf(importance to CanonicalScoreRange(7.0, 9.0))))).containsExactly(orientation.ref)
    }
    @Test fun `missing score never passes numeric predicate even zero minimum`() {
        assertThat(query(CanonicalEntityQuery(scoreFilters = mapOf(importance to CanonicalScoreRange(min = 0.0))))).doesNotContain(workspace.ref)
    }
    @Test fun `multiple score dimensions compose with AND`() {
        assertThat(query(CanonicalEntityQuery(scoreFilters = mapOf(importance to CanonicalScoreRange(min = 7.0), impact to CanonicalScoreRange(max = 5.0))))).containsExactly(orientation.ref)
    }
    @Test fun `tags and scoring never infer ownership across entity classes`() {
        assertThat(query(CanonicalEntityQuery(tags = setOf("health"), scoreFilters = mapOf(importance to CanonicalScoreRange(min = 7.0))))).isEmpty()
    }
    @Test fun `Unplaced and tag compose using exact typed presence`() {
        assertThat(query(CanonicalEntityQuery(unplaced = true, tags = setOf("health")), setOf(workspace.ref))).containsExactly(other.ref)
    }
    @Test fun `Unplaced and scoring compose`() {
        assertThat(query(CanonicalEntityQuery(unplaced = true, scoreFilters = mapOf(importance to CanonicalScoreRange(min = 7.0))), setOf(higher.ref))).containsExactly(orientation.ref)
    }
    @Test fun `title sorting works in both directions`() {
        val asc = query(CanonicalEntityQuery(sort = CanonicalEntitySort(CanonicalEntitySortKey.TITLE)))
        val desc = query(CanonicalEntityQuery(sort = CanonicalEntitySort(CanonicalEntitySortKey.TITLE, CanonicalSortDirection.DESC)))
        assertThat(desc).containsExactlyElementsIn(asc.reversed()).inOrder()
    }
    @Test fun `score sorting keeps missing values last in both directions`() {
        for (direction in CanonicalSortDirection.entries) {
            val result = query(CanonicalEntityQuery(sort = CanonicalEntitySort(CanonicalEntitySortKey.SCORE, direction, importance)))
            assertThat(result.take(2)).containsExactlyElementsIn(if (direction == CanonicalSortDirection.ASC) listOf(orientation.ref, higher.ref) else listOf(higher.ref, orientation.ref)).inOrder()
            assertThat(result.drop(2)).containsExactly(aspect.ref, workspace.ref, other.ref).inOrder()
        }
    }
    @Test fun `equal scores titles and raw ID collisions retain typed identity`() {
        val items = listOf(CanonicalEntityRef.Workspace("same"), CanonicalEntityRef.Aspect("same"), CanonicalEntityRef.Orientation("same"))
            .map { CanonicalEntitySummary(it, "Same", scores = mapOf(importance to 7.0)) }
        val result = executeCanonicalEntityQuery(items + items.first(), CanonicalEntityQuery(sort = CanonicalEntitySort(CanonicalEntitySortKey.SCORE, dimension = importance)))
        assertThat(result.map { it.ref }).containsExactly(items[1].ref, items[2].ref, items[0].ref).inOrder()
    }
    @Test fun `archived admission stays the existing catalog contract`() {
        assertThat(executeCanonicalEntityQuery(listOf(aspect.copy(isArchivedAspect = true)), CanonicalEntityQuery())).isEmpty()
    }
    @Test fun `default sort matches current title ordering`() {
        assertThat(query(CanonicalEntityQuery())).containsExactlyElementsIn(query(CanonicalEntityQuery(sort = CanonicalEntitySort(CanonicalEntitySortKey.TITLE)))).inOrder()
    }
    @Test fun `empty type set is an explicit empty selection`() {
        assertThat(query(CanonicalEntityQuery(entityTypes = emptySet()))).isEmpty()
    }
    @Test fun `timestamp ordering retains full Long precision`() {
        val items = listOf(aspect.copy(createdAt = Long.MAX_VALUE), orientation.copy(createdAt = Long.MAX_VALUE - 1))
        assertThat(sortCanonicalEntityResults(items, CanonicalEntitySort(CanonicalEntitySortKey.CREATED)).map { it.ref }).containsExactly(orientation.ref, aspect.ref).inOrder()
    }
    @Test fun `query sorting does not depend on device locale`() {
        val old = Locale.getDefault()
        try {
            val first = query(CanonicalEntityQuery())
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertThat(query(CanonicalEntityQuery())).isEqualTo(first)
        } finally { Locale.setDefault(old) }
    }
    @Test fun `ranges reject NaN infinity and inversions`() {
        listOf({ CanonicalScoreRange(min = Double.NaN) }, { CanonicalScoreRange(max = Double.POSITIVE_INFINITY) }, { CanonicalScoreRange(8.0, 7.0) }).forEach {
            assertThat(runCatching(it).isFailure).isTrue()
        }
    }
}
