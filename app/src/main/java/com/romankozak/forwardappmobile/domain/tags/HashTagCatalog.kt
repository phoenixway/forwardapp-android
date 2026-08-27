package com.romankozak.forwardappmobile.domain.tags

import java.util.Locale

private val hashTagRegex = Regex("""(^|\s)(#[\p{L}\p{N}_-]+)""")
private val completeHashTagRegex = Regex("""#[\p{L}\p{N}_-]+""")

fun extractHashTags(text: String): List<String> =
    hashTagRegex
        .findAll(text)
        .mapNotNull { match -> match.groups[2]?.value }
        .toList()

fun buildHashTagCatalog(
    texts: Sequence<String>,
    explicitTags: Sequence<String>,
): List<String> =
    (texts.flatMap { text -> extractHashTags(text).asSequence() } +
        explicitTags.mapNotNull(::normalizeExplicitTag))
        .distinctBy { tag -> tag.lowercase(Locale.ROOT) }
        .sortedBy { tag -> tag.lowercase(Locale.ROOT) }
        .toList()

private fun normalizeExplicitTag(tag: String): String? {
    val trimmed = tag.trim()
    if (trimmed.isEmpty()) return null
    val normalized = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    return normalized.takeIf(completeHashTagRegex::matches)
}
