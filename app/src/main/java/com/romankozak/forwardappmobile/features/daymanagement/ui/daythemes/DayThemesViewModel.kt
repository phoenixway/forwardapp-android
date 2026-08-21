package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DayThemesViewModel
    @Inject
    constructor(
        private val repository: DayThemeRepository,
    ) : ViewModel() {
        private val planId = MutableStateFlow<String?>(null)

        val uiState =
            planId.flatMapLatest { currentPlanId ->
                if (currentPlanId == null) {
                    flowOf(DayThemesUiState())
                } else {
                    repository.observe(currentPlanId).map { document ->
                        DayThemesUiState(
                            dayPlanId = currentPlanId,
                            themes = document.themes.sortedWith(compareBy(DayTheme::order, DayTheme::createdAt)),
                            assignments = document.assignments.associate { it.entityId to it.themeIds },
                            isLoading = false,
                        )
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DayThemesUiState(),
            )

        fun loadPlan(dayPlanId: String) {
            planId.value = dayPlanId
            viewModelScope.launch { repository.migrateLegacyDayIfNeeded(dayPlanId) }
        }

        fun saveTheme(themeId: String?, draft: DayThemeDraft) {
            val dayPlanId = planId.value ?: return
            val safeDraft = draft.copy(title = draft.title.trim(), budgetPercent = draft.budgetPercent.coerceIn(0, 100))
            if (safeDraft.title.isBlank()) return
            viewModelScope.launch {
                repository.update(dayPlanId) { document ->
                    val now = System.currentTimeMillis()
                    val existing = document.themes.firstOrNull { it.id == themeId }
                    val saved =
                        existing?.copy(
                            title = safeDraft.title,
                            colorArgb = safeDraft.colorArgb,
                            iconKey = safeDraft.iconKey,
                            comment = safeDraft.comment.trim(),
                            budgetPercent = safeDraft.budgetPercent,
                            updatedAt = now,
                        ) ?: DayTheme(
                            dayPlanId = dayPlanId,
                            title = safeDraft.title,
                            colorArgb = safeDraft.colorArgb,
                            iconKey = safeDraft.iconKey,
                            comment = safeDraft.comment.trim(),
                            budgetPercent = safeDraft.budgetPercent,
                            order = (document.themes.maxOfOrNull(DayTheme::order) ?: -1L) + 1L,
                            createdAt = now,
                            updatedAt = now,
                        )
                    document.copy(themes = document.themes.filterNot { it.id == saved.id } + saved)
                }
            }
        }

        fun deleteTheme(themeId: String) {
            val dayPlanId = planId.value ?: return
            viewModelScope.launch {
                repository.update(dayPlanId) { document ->
                    document.copy(
                        themes = document.themes.filterNot { it.id == themeId },
                        assignments =
                            document.assignments.mapNotNull { assignment ->
                                val remaining = assignment.themeIds - themeId
                                assignment.copy(themeIds = remaining).takeIf { remaining.isNotEmpty() }
                            },
                    )
                }
            }
        }

        fun toggleTheme(entityId: String, themeId: String) {
            val dayPlanId = planId.value ?: return
            viewModelScope.launch {
                repository.update(dayPlanId) { document ->
                    val current =
                        document.assignments.firstOrNull {
                            it.dayPlanId == dayPlanId && it.entityId == entityId
                        }
                    val selected = current?.themeIds.orEmpty()
                    val changed = if (themeId in selected) selected - themeId else selected + themeId
                    val other =
                        document.assignments.filterNot {
                            it.dayPlanId == dayPlanId && it.entityId == entityId
                        }
                    document.copy(
                        assignments =
                            if (changed.isEmpty()) other else other + DayThemeAssignment(dayPlanId, entityId, changed),
                    )
                }
            }
        }

        fun setThemeActive(themeId: String, active: Boolean) {
            val dayPlanId = planId.value ?: return
            viewModelScope.launch {
                repository.update(dayPlanId) { document ->
                    document.copy(
                        themes = document.themes.map { theme ->
                            if (theme.id == themeId) theme.copy(isActive = active, updatedAt = System.currentTimeMillis()) else theme
                        },
                    )
                }
            }
        }

        fun reorderThemes(orderedIds: List<String>) {
            val dayPlanId = planId.value ?: return
            viewModelScope.launch {
                repository.update(dayPlanId) { document ->
                    val orderById = orderedIds.withIndex().associate { (index, id) -> id to index.toLong() }
                    document.copy(
                        themes = document.themes.map { theme ->
                            orderById[theme.id]?.let { theme.copy(order = it, updatedAt = System.currentTimeMillis()) } ?: theme
                        },
                    )
                }
            }
        }
    }
