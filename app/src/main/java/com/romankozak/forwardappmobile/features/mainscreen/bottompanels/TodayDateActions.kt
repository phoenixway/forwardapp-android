package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.WbSunny
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

internal fun todayDateActions(
    tab: DayManagementTab,
    onCalendar: () -> Unit,
    onWakeUp: () -> Unit,
    onSleep: () -> Unit,
): List<MoreSheetAction> = buildList {
    add(MoreSheetAction("Вибрати день", onCalendar, Icons.Outlined.CalendarToday))
    if (tab == DayManagementTab.DAY_START) {
        add(MoreSheetAction("Проснувся!", onWakeUp, Icons.Outlined.WbSunny))
        add(MoreSheetAction("Пішов спати", onSleep, Icons.Outlined.Bedtime))
    }
}
