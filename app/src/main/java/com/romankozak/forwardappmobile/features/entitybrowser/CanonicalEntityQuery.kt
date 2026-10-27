package com.romankozak.forwardappmobile.features.entitybrowser

import java.util.Locale

/** Numeric coordinates from orientation-default policy V1, not ordinal enum indices. */
enum class CanonicalScoreDimension(val code: String) {
    IMPORTANCE("importance"), IMPACT("impact"), EFFORT("effort"), COST("cost"), RISK("risk"), SCORE("score");
    companion object { fun parse(code: String) = entries.firstOrNull { it.code == code.lowercase(Locale.ROOT) } }
}

data class CanonicalScoreRange(val min: Double? = null, val max: Double? = null) {
    init {
        require(min == null || min.isFinite())
        require(max == null || max.isFinite())
        require(min == null || max == null || min <= max)
    }
    fun contains(value: Double?) = value != null && value.isFinite() &&
        (min == null || value >= min) && (max == null || value <= max)
}

enum class CanonicalEntitySortKey { DEFAULT, TITLE, CREATED, UPDATED, SCORE }
enum class CanonicalSortDirection { ASC, DESC }
data class CanonicalEntitySort(
    val key: CanonicalEntitySortKey = CanonicalEntitySortKey.DEFAULT,
    val direction: CanonicalSortDirection = CanonicalSortDirection.ASC,
    val dimension: CanonicalScoreDimension? = null,
) { init { require(key != CanonicalEntitySortKey.SCORE || dimension != null) } }

/** Read-only specification shared by graphical and textual adapters. All predicates compose with AND. */
data class CanonicalEntityQuery(
    val text: String = "",
    val entityTypes: Set<CanonicalEntityClass> = CanonicalEntityClass.entries.toSet(),
    val tags: Set<String> = emptySet(),
    val scoreFilters: Map<CanonicalScoreDimension, CanonicalScoreRange> = emptyMap(),
    val sort: CanonicalEntitySort = CanonicalEntitySort(),
    val unplaced: Boolean = false,
)

fun normalizeCanonicalQueryTag(value: String) = value.trim().removePrefix("#").lowercase(Locale.ROOT)

fun executeCanonicalEntityQuery(
    catalog: List<CanonicalEntitySummary>,
    query: CanonicalEntityQuery,
    placed: Set<CanonicalEntityRef> = emptySet(),
): List<CanonicalEntitySummary> {
    val tags = query.tags.map(::normalizeCanonicalQueryTag).filter(String::isNotEmpty).toSet()
    val admitted = filterCanonicalEntities(catalog, CanonicalEntityBrowserQuery(allowedClasses = query.entityTypes))
        .asSequence().filter { !query.unplaced || it.ref !in placed }
        .filter { item -> tags.all { it in item.tags } }
        .filter { item -> query.scoreFilters.all { (dimension, range) -> range.contains(item.scores[dimension]) } }
        .filter { canonicalTitleMatches(it.title, query.text) }
        .distinctBy { it.ref }.toList()
    return sortCanonicalEntityResults(admitted, query.sort)
}

fun sortCanonicalEntityResults(items: List<CanonicalEntitySummary>, sort: CanonicalEntitySort): List<CanonicalEntitySummary> {
    val tie = compareBy<CanonicalEntitySummary>({ it.title.trim().lowercase(Locale.ROOT) }, { it.ref.entityClass.name }, { it.ref.id })
    val primary = Comparator<CanonicalEntitySummary> { a, b ->
        if (sort.key == CanonicalEntitySortKey.DEFAULT || sort.key == CanonicalEntitySortKey.TITLE) {
            val result = a.title.trim().lowercase(Locale.ROOT).compareTo(b.title.trim().lowercase(Locale.ROOT))
            if (sort.direction == CanonicalSortDirection.ASC) result else -result
        } else if (sort.key == CanonicalEntitySortKey.SCORE) {
            val left = a.scores[sort.dimension]?.takeIf { it.isFinite() }
            val right = b.scores[sort.dimension]?.takeIf { it.isFinite() }
            when {
                left == null && right == null -> 0
                left == null -> 1
                right == null -> -1
                sort.direction == CanonicalSortDirection.ASC -> left.compareTo(right)
                else -> right.compareTo(left)
            }
        } else {
            val left = if (sort.key == CanonicalEntitySortKey.CREATED) a.createdAt else a.updatedAt
            val right = if (sort.key == CanonicalEntitySortKey.CREATED) b.createdAt else b.updatedAt
            when {
                left == null && right == null -> 0
                left == null -> 1
                right == null -> -1
                sort.direction == CanonicalSortDirection.ASC -> left.compareTo(right)
                else -> right.compareTo(left)
            }
        }
    }
    return items.sortedWith(primary.then(tie))
}
