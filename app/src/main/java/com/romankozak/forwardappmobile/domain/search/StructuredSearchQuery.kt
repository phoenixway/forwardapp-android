package com.romankozak.forwardappmobile.domain.search

import java.util.Locale

data class StructuredSearchQuery(
    val raw: String,
    val textQuery: String,
    val tags: List<String>,
) {
    val hasTags: Boolean
        get() = tags.isNotEmpty()

    fun matches(searchableTexts: List<String>): Boolean {
        val corpus = searchableTexts.joinToString("\n").lowercase(Locale.getDefault())
        val availableTags = extractHashtags(corpus)
        val tagsMatch =
            tags.all { requestedTag ->
                availableTags.any { availableTag -> availableTag.contains(requestedTag) }
            }
        if (!tagsMatch) return false

        return textQuery
            .split(WHITESPACE_REGEX)
            .filter { it.isNotBlank() }
            .all { term -> corpus.contains(term.lowercase(Locale.getDefault())) }
    }

    fun matchedTags(searchableTexts: List<String>): List<String> {
        if (!hasTags) return emptyList()
        val availableTags = searchableTexts.flatMap(::extractHashtags).distinct()
        return availableTags.filter { availableTag ->
            tags.any { requestedTag -> availableTag.contains(requestedTag) }
        }
    }

    companion object {
        private val QUERY_TAG_REGEX = Regex("""(?<![#\p{L}\p{N}_-])#([\p{L}\p{N}_-]+)""")
        private val WHITESPACE_REGEX = Regex("""\s+""")

        fun parse(rawQuery: String): StructuredSearchQuery {
            val sanitized = rawQuery.removePrefix("%").removeSuffix("%").trim()
            val tags =
                QUERY_TAG_REGEX
                    .findAll(sanitized)
                    .map { it.groupValues[1].lowercase(Locale.getDefault()) }
                    .distinct()
                    .toList()
            val textQuery = QUERY_TAG_REGEX.replace(sanitized, " ").replace(WHITESPACE_REGEX, " ").trim()
            return StructuredSearchQuery(raw = sanitized, textQuery = textQuery, tags = tags)
        }

        fun extractHashtags(text: String): List<String> =
            QUERY_TAG_REGEX
                .findAll(text)
                .map { it.groupValues[1].lowercase(Locale.getDefault()) }
                .distinct()
                .toList()
    }
}
