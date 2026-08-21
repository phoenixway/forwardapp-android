package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import java.util.UUID

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
