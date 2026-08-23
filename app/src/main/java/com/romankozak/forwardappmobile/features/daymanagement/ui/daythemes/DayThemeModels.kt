package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import java.util.UUID

/**
 * UI compatibility projection of canonical ThemeDefinition + DayTheme.
 * Persisted ids are canonical DayTheme ids. The UUID default is only a temporary
 * client id for a newly-created theme before the repository materializes it.
 */
data class DayTheme(
    val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val title: String,
    val colorArgb: Long = 0xFF2563EB,
    val iconKey: String = "target",
    val comment: String = "",
    val budgetPercent: Int = 0,
    val order: Long = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class DayThemeAssignment(
    val dayPlanId: String,
    val entityId: String,
    val themeIds: Set<String> = emptySet(),
)

data class DayThemeDocument(
    val themes: List<DayTheme> = emptyList(),
    val assignments: List<DayThemeAssignment> = emptyList(),
)

data class DayThemesUiState(
    val dayPlanId: String? = null,
    val themes: List<DayTheme> = emptyList(),
    val assignments: Map<String, Set<String>> = emptyMap(),
    val isLoading: Boolean = true,
) {
    val activeThemes: List<DayTheme> get() = themes.filter(DayTheme::isActive)
    val totalBudgetPercent: Int get() = activeThemes.sumOf(DayTheme::budgetPercent)

    fun themesFor(entityId: String): List<DayTheme> {
        val selected = assignments[entityId].orEmpty()
        return activeThemes.filter { it.id in selected }
    }
}

data class DayThemeDraft(
    val title: String,
    val colorArgb: Long,
    val iconKey: String,
    val comment: String,
    val budgetPercent: Int,
)

internal fun canonicalDayThemeIconKey(key: String): String =
    when (key) {
        "spark" -> "sparkles"
        "mind" -> "brain"
        "flag" -> "target"
        else -> key
    }

/**
 * Compatibility projection normalization for the existing Day Themes UI.
 * Canonical persistence owns identity; this helper only normalizes UI day scope
 * and legacy icon aliases.
 */
internal fun DayThemeDocument.normalizedForDay(dayPlanId: String): DayThemeDocument =
    copy(
        themes = themes.map { theme ->
            theme.copy(
                dayPlanId = dayPlanId,
                iconKey = canonicalDayThemeIconKey(theme.iconKey),
            )
        },
        assignments = assignments.map { assignment ->
            assignment.copy(dayPlanId = dayPlanId)
        },
    )
