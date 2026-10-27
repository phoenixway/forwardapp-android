package com.romankozak.forwardappmobile.features.entitybrowser

import java.util.Locale

enum class CanonicalQueryError { UNKNOWN_OPERATOR, UNKNOWN_ENTITY_TYPE, UNKNOWN_SCORE_DIMENSION, MALFORMED_NUMBER, INVALID_SORT, INVALID_TAG }

data class CanonicalEntityQueryParseResult(
    val query: CanonicalEntityQuery,
    val errors: List<String>,
    val errorKinds: Set<CanonicalQueryError> = emptySet(),
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/** Thin MagicBox adapter. Invalid operators are reported, never silently widened into all entities. */
fun parseCanonicalEntityQuery(input: String): CanonicalEntityQueryParseResult {
    val text = mutableListOf<String>()
    val types = mutableSetOf<CanonicalEntityClass>()
    val tags = mutableSetOf<String>()
    val scores = mutableMapOf<CanonicalScoreDimension, CanonicalScoreRange>()
    var sort = CanonicalEntitySort()
    var unplaced = false
    val errors = mutableListOf<String>()
    val kinds = mutableSetOf<CanonicalQueryError>()
    fun error(kind: CanonicalQueryError, message: String) { kinds += kind; errors += message }
    input.trim().split(Regex("\\s+")).filter(String::isNotEmpty).forEach { token ->
        when {
            token.startsWith("#") -> {
                val tag = normalizeCanonicalQueryTag(token)
                if (tag.isEmpty()) error(CanonicalQueryError.INVALID_TAG, "Порожній тег") else tags += tag
            }
            token.startsWith("type:", true) -> {
                val type = CanonicalEntityClass.entries.firstOrNull { it.name.equals(token.substringAfter(':'), true) }
                if (type == null) error(CanonicalQueryError.UNKNOWN_ENTITY_TYPE, "Невідомий тип: $token") else types += type
            }
            token.startsWith("score:", true) -> {
                val parts = Regex("score:([a-z]+)(>=|<=)([-+]?[0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE).matchEntire(token)
                val dimension = parts?.groupValues?.get(1)?.let(CanonicalScoreDimension::parse)
                val number = parts?.groupValues?.get(3)?.toDoubleOrNull()?.takeIf { it.isFinite() }
                if (parts == null || number == null) error(CanonicalQueryError.MALFORMED_NUMBER, "Невірний scoring predicate: $token")
                else if (dimension == null) error(CanonicalQueryError.UNKNOWN_SCORE_DIMENSION, "Невідомий scoring dimension: $token")
                else {
                    val old = scores[dimension] ?: CanonicalScoreRange()
                    val min = if (parts.groupValues[2] == ">=") maxOf(old.min ?: number, number) else old.min
                    val max = if (parts.groupValues[2] == "<=") minOf(old.max ?: number, number) else old.max
                    if (min != null && max != null && min > max) error(CanonicalQueryError.MALFORMED_NUMBER, "Порожній scoring range: $token")
                    else scores[dimension] = CanonicalScoreRange(min, max)
                }
            }
            token.startsWith("sort:", true) -> {
                val parts = token.substringAfter(':').split(':')
                val key = parts.first().lowercase(Locale.ROOT)
                val dimension = CanonicalScoreDimension.parse(key)
                val direction = when (parts.getOrNull(1)?.lowercase(Locale.ROOT)) {
                    null, "asc" -> CanonicalSortDirection.ASC
                    "desc" -> CanonicalSortDirection.DESC
                    else -> null
                }
                val field = when (key) {
                    "default" -> CanonicalEntitySortKey.DEFAULT
                    "title", "name" -> CanonicalEntitySortKey.TITLE
                    "created" -> CanonicalEntitySortKey.CREATED
                    "updated" -> CanonicalEntitySortKey.UPDATED
                    else -> if (dimension != null) CanonicalEntitySortKey.SCORE else null
                }
                if (field == null || direction == null || parts.size > 2) error(
                    if (field == null) CanonicalQueryError.UNKNOWN_SCORE_DIMENSION else CanonicalQueryError.INVALID_SORT, "Невірне сортування: $token")
                else sort = CanonicalEntitySort(field, direction, dimension)
            }
            token.equals("is:unplaced", true) -> unplaced = true
            ':' in token -> error(CanonicalQueryError.UNKNOWN_OPERATOR, "Невідомий оператор: $token")
            else -> text += token
        }
    }
    return CanonicalEntityQueryParseResult(CanonicalEntityQuery(text.joinToString(" "),
        if (errors.isNotEmpty()) emptySet() else types.ifEmpty { CanonicalEntityClass.entries.toSet() }, tags, scores, sort, unplaced), errors, kinds)
}
