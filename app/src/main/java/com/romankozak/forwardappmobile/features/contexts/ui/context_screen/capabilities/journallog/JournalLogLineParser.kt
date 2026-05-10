package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog

import androidx.compose.ui.text.font.FontWeight

internal data class ParsedJournalLine(
    val marker: String?,
    val text: String,
    val textWeight: FontWeight = FontWeight.Normal,
)

internal fun parseJournalLine(raw: String): ParsedJournalLine? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(Regex("\\s+"), limit = 2)
    if (parts.size < 2) return ParsedJournalLine(marker = null, text = trimmed)

    val token = parts[0]
    val text = parts[1].trim()
    if (!token.looksLikeJournalMarker()) {
        return ParsedJournalLine(marker = null, text = trimmed)
    }

    val weight =
        when (token) {
            "!!", "!!!" -> FontWeight.ExtraBold
            "!" -> FontWeight.Bold
            else -> FontWeight.Normal
        }

    return ParsedJournalLine(marker = token, text = text, textWeight = weight)
}

private fun String.looksLikeJournalMarker(): Boolean {
    if (length !in 1..3) return false
    if (any(Char::isWhitespace)) return false

    if (all { !it.isLetterOrDigit() }) return true
    if (length == 1 && this in SINGLE_LETTER_MARKERS) return true
    if (all(::isAsciiLetterOrDigit)) return true

    return false
}

private val SINGLE_LETTER_MARKERS =
    setOf(
        "i",
        "t",
        "і",
        "и",
        "т",
    )

private fun isAsciiLetterOrDigit(char: Char): Boolean =
    char in 'a'..'z' ||
        char in 'A'..'Z' ||
        char in '0'..'9'
