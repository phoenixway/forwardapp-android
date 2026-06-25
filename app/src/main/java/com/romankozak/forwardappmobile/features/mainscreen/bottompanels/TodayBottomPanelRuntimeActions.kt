package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab

@Composable
fun TodayBottomPanelRuntimeActions(
    currentTab: DayManagementTab,
    runtimeUiState: DayManagementRuntimeUiState,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasOpenDay = runtimeUiState.runtimeState.hasOpenOperationalDay
    val finalizationStarted = runtimeUiState.runtimeState.finalizationStartedAt != null
    when (currentTab) {
        DayManagementTab.DAY_START -> Unit
        DayManagementTab.DAY_PLAN -> Unit
        DayManagementTab.JOURNAL -> Unit
        DayManagementTab.DAY_FOCUSES -> Unit
        DayManagementTab.FINALIZATION ->
            Button(
                onClick = if (finalizationStarted) onSleep else onStartFinalization,
                modifier = modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    when {
                        finalizationStarted -> "Закінчити день"
                        hasOpenDay -> "Почати фіналізацію"
                        else -> "Стартувати день і фіналізацію"
                    },
                )
            }
        else -> Unit
    }
}
