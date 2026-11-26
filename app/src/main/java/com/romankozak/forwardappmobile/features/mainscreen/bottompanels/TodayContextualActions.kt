package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.TaskAlt
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

internal fun todayContextualActions(
    selected: DayManagementTab,
    additional: List<MoreSheetAction>,
    runtime: DayManagementRuntimeUiState,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
): List<ContextualAction> = buildList {
    additional.forEachIndexed { index, action ->
        add(ContextualAction("today-action-$index", action.label, icon = action.icon, onClick = action.onClick))
    }
    if (selected == DayManagementTab.FINALIZATION) {
        val started = runtime.runtimeState.finalizationStartedAt != null
        add(ContextualAction("finalize", when {
            started -> "Закінчити день"
            runtime.runtimeState.hasOpenOperationalDay -> "Почати фіналізацію"
            else -> "Стартувати день і фіналізацію"
        }, icon = if (started) Icons.Outlined.Bedtime else Icons.Outlined.TaskAlt,
            onClick = if (started) onSleep else onStartFinalization))
    }
}
