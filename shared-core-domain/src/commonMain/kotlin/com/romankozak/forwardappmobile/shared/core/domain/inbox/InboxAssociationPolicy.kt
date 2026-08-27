@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.inbox

import kotlin.js.JsExport
import kotlin.js.JsName

fun normalizeInboxAssociationTag(tag: String): String =
    tag.trim().removePrefix("#").lowercase()

fun extractInboxAssociationHashtags(text: String): List<String> {
    val result = LinkedHashSet<String>()
    var index = 0

    while (index < text.length) {
        if (text[index] != '#') {
            index++
            continue
        }

        val start = index + 1
        if (start >= text.length || !text[start].isLetter()) {
            index++
            continue
        }

        var end = start + 1
        while (end < text.length) {
            val char = text[end]
            if (!(char.isLetterOrDigit() || char == '_' || char == '-')) break
            end++
        }

        result += text.substring(start, end).lowercase()
        index = end
    }

    return result.toList()
}

fun firstMatchingInboxAssociationTag(
    text: String,
    contextTags: List<String>,
): String? {
    val hashtags = extractInboxAssociationHashtags(text).toSet()
    if (hashtags.isEmpty()) return null

    return contextTags
        .asSequence()
        .map(::normalizeInboxAssociationTag)
        .firstOrNull { it.isNotBlank() && it in hashtags }
}

fun inboxTextMatchesContextTags(
    text: String,
    contextTags: List<String>,
): Boolean = firstMatchingInboxAssociationTag(text, contextTags) != null

fun inboxOwnerVisible(
    removeAfterTagAutocopy: Boolean,
    hasForeignAssociation: Boolean,
): Boolean = !removeAfterTagAutocopy || !hasForeignAssociation

@JsExport
@JsName("inboxTextMatchesContextTags")
fun inboxTextMatchesContextTagsForJs(
    text: String,
    contextTags: Array<String>,
): Boolean = inboxTextMatchesContextTags(text, contextTags.toList())

@JsExport
@JsName("inboxOwnerVisible")
fun inboxOwnerVisibleForJs(
    removeAfterTagAutocopy: Boolean,
    hasForeignAssociation: Boolean,
): Boolean = inboxOwnerVisible(removeAfterTagAutocopy, hasForeignAssociation)
