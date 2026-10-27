package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

/** Only the task-list composer moves its actions; other Day surfaces keep their existing layout. */
internal fun todayTaskActionsAboveInput(tab: DayManagementTab): Boolean = tab == DayManagementTab.DAY_PLAN

/** Visible task creation entry; other Day tabs retain their own composer semantics. */
internal fun todayTaskSourceAction(tab: DayManagementTab, onAdd: () -> Unit): MoreSheetAction? =
    when (tab) {
        DayManagementTab.DAY_START, DayManagementTab.DAY_PLAN ->
            MoreSheetAction("Створити завдання на основі сутності", onAdd)
        else -> null
    }
