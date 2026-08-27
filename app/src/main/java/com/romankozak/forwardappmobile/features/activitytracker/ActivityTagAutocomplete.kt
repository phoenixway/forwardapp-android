package com.romankozak.forwardappmobile.features.activitytracker

import com.romankozak.forwardappmobile.domain.tags.extractHashTags
import java.util.Locale

private val activeHashTagQueryRegex = Regex("""(^|\s)(#[\p{L}\p{N}_-]*)$""")

internal fun extractActivityTags(text: String): List<String> = extractHashTags(text)

internal fun buildActivityTagSuggestions(
    inputText: String,
    knownTags: List<String>,
    limit: Int = 8,
): List<String> {
    val activeQuery = activeHashTagQueryRegex.find(inputText)?.groups?.get(2)?.value ?: return emptyList()

    return knownTags
        .asSequence()
        .map { tag -> if (tag.startsWith("#")) tag else "#$tag" }
        .filter { tag -> tag.startsWith(activeQuery, ignoreCase = true) }
        .distinctBy { tag -> tag.lowercase(Locale.ROOT) }
        .sortedBy { tag -> tag.lowercase(Locale.ROOT) }
        .take(limit)
        .toList()
}

internal fun applyActivityTagSuggestion(
    inputText: String,
    suggestion: String,
): String {
    val activeQuery = activeHashTagQueryRegex.find(inputText)?.groups?.get(2) ?: return inputText
    val normalizedSuggestion = if (suggestion.startsWith("#")) suggestion else "#$suggestion"
    return inputText.replaceRange(activeQuery.range, normalizedSuggestion) + " "
}
