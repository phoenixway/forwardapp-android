package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

@Suppress("LongParameterList")
internal data class TodayMoreActionCallbacks(
    val onWakeUp: () -> Unit,
    val onSleep: () -> Unit,
    val onSetPredictedDayDuration: () -> Unit,
    val onFinalizePlan: () -> Unit,
    val onFinalizeFocus: () -> Unit,
    val onStartImplementation: () -> Unit,
    val onAddTaskFromContext: () -> Unit,
    val onAddFocus: () -> Unit,
    val onAddResponsibility: () -> Unit,
    val onOpenQuickDoneDialog: () -> Unit,
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
                ),
            )

        DayManagementTab.DAY_PLAN ->
            listOf(
                MoreSheetAction(
                    label = "Додати задачу з контексту",
                    onClick = callbacks.onAddTaskFromContext,
                ),
                MoreSheetAction(
                    label =
                        if (runtimeUiState.runtimeState.dayPlanFinalizedAt != null) {
                            "План дня зафіксовано"
                        } else {
                            "План дня готовий"
                        },
                    onClick = callbacks.onFinalizePlan,
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
                ),
                MoreSheetAction(
                    label = "Додати фокус",
                    onClick = callbacks.onAddFocus,
                ),
                MoreSheetAction(
                    label = "Додати зону відповідальності",
                    onClick = callbacks.onAddResponsibility,
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
                ),
                MoreSheetAction(
                    label = "Події",
                    onClick = callbacks.onOpenQuickDoneDialog,
                ),
                MoreSheetAction(
                    label = "Коментар",
                    onClick = callbacks.onTimelessRecordClick,
                ),
                MoreSheetAction(
                    label = "Експорт в Markdown",
                    onClick = callbacks.onExportJournalToMarkdown,
                ),
                MoreSheetAction(
                    label = "Очистити лог",
                    onClick = callbacks.onClearJournal,
                ),
            )

        else -> emptyList()
    }
