package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.JsonObject

/**
 * Presence state of the canonical Day Themes wire contract.
 *
 * This deliberately classifies the raw JSON object rather than SnapshotBundle:
 * raw JSON preserves the distinction between an absent field and an explicit
 * null/non-array value.
 */
enum class DayThemeCanonicalPresence {
    LEGACY,
    CANONICAL,
    MALFORMED,
}

private val CANONICAL_DAY_THEME_FIELDS =
    listOf(
        "themeDefinitions",
        "dayThemes",
        "dayThemeAssignmentDocuments",
    )

fun classifyDayThemeCanonicalPresence(json: JsonObject): DayThemeCanonicalPresence {
    val presentCount = CANONICAL_DAY_THEME_FIELDS.count(json::has)

    if (presentCount == 0) return DayThemeCanonicalPresence.LEGACY
    if (presentCount != CANONICAL_DAY_THEME_FIELDS.size) return DayThemeCanonicalPresence.MALFORMED

    return if (CANONICAL_DAY_THEME_FIELDS.all { field -> json.get(field).isJsonArray }) {
        DayThemeCanonicalPresence.CANONICAL
    } else {
        DayThemeCanonicalPresence.MALFORMED
    }
}
