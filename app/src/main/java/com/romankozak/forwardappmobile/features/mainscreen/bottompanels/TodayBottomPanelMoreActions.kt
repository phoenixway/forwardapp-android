package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

@Suppress("LongParameterList")
internal data class TodayMoreActionCallbacks(
    val onWakeUp: () -> Unit,
    val onSleep: () -> Unit,
    val onSetPredictedDayDuration: () -> Unit,
    val onFinalizePlan: () -> Unit,
    val onFinalizeThemes: () -> Unit,
    val onFinalizeFocus: () -> Unit,
    val onStartImplementation: () -> Unit,
    val onAddTaskFromContext: () -> Unit,
    val onAddPriority: () -> Unit,
    val onOpenQuickDoneDialog: () -> Unit,
    val onOpenBackdatedDialog: () -> Unit,
    val onTimelessRecordClick: () -> Unit,
    val onExportJournalToMarkdown: () -> Unit,
    val onClearJournal: () -> Unit,
)

internal fun buildTodayAdditionalMoreActions(
    currentTab: DayManagementTab,
    runtimeUiState: DayManagementRuntimeUiState,
    callbacks: TodayMoreActionCallbacks,
): List<MoreSheetAction> =
    when (currentTab) {
        DayManagementTab.DAY_START ->
            listOf(
                MoreSheetAction(
                    label = "Задати прогнозовану тривалість дня",
                    onClick = callbacks.onSetPredictedDayDuration,
                    icon = Icons.Outlined.Schedule,
                ),
            )

        DayManagementTab.DAY_PLAN ->
            listOf(
                MoreSheetAction(
                    label = "Створити завдання на основі сутності",
                    onClick = callbacks.onAddTaskFromContext,
                    icon = Icons.Outlined.Add,
                ),
                MoreSheetAction(
                    label =
                        if (runtimeUiState.runtimeState.dayPlanFinalizedAt != null) {
                            "План дня зафіксовано"
                        } else {
                            "План дня готовий"
                        },
                    onClick = callbacks.onFinalizePlan,
                    icon = Icons.Outlined.CheckCircle,
                ),
            )

        DayManagementTab.DAY_THEMES ->
            listOf(
                MoreSheetAction(
                    label =
                        if (runtimeUiState.runtimeState.dayThemesFinalizedAt != null) {
                            "Теми дня зафіксовано"
                        } else {
                            "Теми дня зафіксувати"
                        },
                    onClick = callbacks.onFinalizeThemes,
                    icon = Icons.Outlined.CheckCircle,
                ),
            )

        DayManagementTab.DAY_FOCUSES ->
            listOf(
                MoreSheetAction(
                    label =
                        if (runtimeUiState.runtimeState.dayFocusFinalizedAt != null) {
                            "Фокус дня зафіксований"
                        } else {
                            "Фокус дня зафіксувати"
                        },
                    onClick = callbacks.onFinalizeFocus,
                    icon = Icons.Outlined.CheckCircle,
                ),
                MoreSheetAction(
                    label = "Додати пріоритет",
                    onClick = callbacks.onAddPriority,
                    icon = Icons.Outlined.Add,
                ),
            )

        DayManagementTab.JOURNAL ->
            listOf(
                MoreSheetAction(
                    label =
                        if (runtimeUiState.runtimeState.hasOpenOperationalDay) {
                            "Почати реалізацію"
                        } else {
                            "Стартувати день і реалізацію"
                        },
                    onClick = callbacks.onStartImplementation,
                    icon = Icons.Outlined.PlayArrow,
                ),
                MoreSheetAction(
                    label = "Подія без тривалості",
                    onClick = callbacks.onOpenQuickDoneDialog,
                    icon = Icons.Outlined.CheckCircle,
                ),
                MoreSheetAction(
                    label = "Додати минулу активність",
                    onClick = callbacks.onOpenBackdatedDialog,
                    icon = Icons.Outlined.History,
                ),
                MoreSheetAction(
                    label = "Коментар",
                    onClick = callbacks.onTimelessRecordClick,
                    icon = Icons.Outlined.AddComment,
                ),
                MoreSheetAction(
                    label = "Експорт в Markdown",
                    onClick = callbacks.onExportJournalToMarkdown,
                    icon = Icons.Outlined.ContentCopy,
                ),
                MoreSheetAction(
                    label = "Очистити лог",
                    onClick = callbacks.onClearJournal,
                    icon = Icons.Outlined.DeleteOutline,
                ),
            )

        else -> emptyList()
    }
