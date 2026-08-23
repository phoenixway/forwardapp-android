package com.romankozak.forwardappmobile.shared.core.models.day

/**
 * Stable canonical identity for one ThemeDefinition materialized on one DayPlan.
 *
 * String.length is intentionally used on both Kotlin and TypeScript sides:
 * JVM and JS both count UTF-16 code units, which keeps the encoding identical
 * without crypto, escaping, or platform-specific infrastructure.
 */
fun canonicalDayThemeId(dayPlanId: String, themeId: String): String =
    "day_theme:${dayPlanId.length}:$dayPlanId:${themeId.length}:$themeId"
