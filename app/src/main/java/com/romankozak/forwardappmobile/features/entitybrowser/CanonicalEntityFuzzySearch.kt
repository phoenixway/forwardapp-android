package com.romankozak.forwardappmobile.features.entitybrowser

private val titleWords = Regex("[^\\p{L}\\p{N}]+")
private val queryWords = Regex("\\s+")

/** Token matching tolerates typos while keeping short queries precise. No database reads. */
internal fun canonicalTitleMatches(title: String, query: String): Boolean {
    val normalized = title.lowercase()
    val needle = query.trim().lowercase()
    if (needle.isEmpty() || normalized.contains(needle)) return true
    val words = normalized.split(titleWords).filter(String::isNotEmpty)
    return needle.split(queryWords).all { term ->
        words.any { word ->
            word.contains(term) || withinEditDistance(
                term,
                word,
                when {
                    term.length < 3 -> 0
                    term.length < 6 -> 1
                    else -> 2
                },
            )
        }
    }
}

private fun withinEditDistance(left: String, right: String, limit: Int): Boolean {
    if (kotlin.math.abs(left.length - right.length) > limit) return false
    var previous = IntArray(right.length + 1) { it }
    for (i in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = i + 1
        for (j in right.indices) {
            current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + if (left[i] == right[j]) 0 else 1,
            )
        }
        if (current.minOrNull()!! > limit) return false
        previous = current
    }
    return previous[right.length] <= limit
}
