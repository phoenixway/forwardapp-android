package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

internal fun todayDateActions(
    tab: DayManagementTab,
    onCalendar: () -> Unit,
    onWakeUp: () -> Unit,
    onSleep: () -> Unit,
): List<MoreSheetAction> = buildList {
    add(MoreSheetAction("Вибрати день", onCalendar))
    if (tab == DayManagementTab.DAY_START) {
        add(MoreSheetAction("Проснувся!", onWakeUp))
        add(MoreSheetAction("Пішов спати", onSleep))
    }
}
