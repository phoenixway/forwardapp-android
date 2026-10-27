package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

internal fun todayContextualActions(
    selected: DayManagementTab,
    additional: List<MoreSheetAction>,
    runtime: DayManagementRuntimeUiState,
    onSelectTab: (DayManagementTab) -> Unit,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
): List<ContextualAction> = buildList {
    additional.forEachIndexed { index, action ->
        add(ContextualAction("today-action-$index", action.label, onClick = action.onClick))
    }
    if (selected == DayManagementTab.FINALIZATION) {
        val started = runtime.runtimeState.finalizationStartedAt != null
        add(ContextualAction("finalize", when {
            started -> "Закінчити день"
            runtime.runtimeState.hasOpenOperationalDay -> "Почати фіналізацію"
            else -> "Стартувати день і фіналізацію"
        }, onClick = if (started) onSleep else onStartFinalization))
    }
    DayManagementTab.todaySubTabs().forEach { tab ->
        add(ContextualAction("today-mode-${tab.name}", tab.title, tab.icon, selected = selected == tab,
            onClick = { onSelectTab(tab) }))
    }
}
