package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onWakeUp: () -> Unit,
    onFinalizePlan: () -> Unit,
    onStartImplementation: () -> Unit,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasOpenDay = runtimeUiState.runtimeState.hasOpenOperationalDay
    when (currentTab) {
        DayManagementTab.DAY_START ->
            Row(
                modifier = modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onWakeUp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Проснувся!")
                }
                OutlinedButton(
                    onClick = onSleep,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Пішов спати")
                }
            }
        DayManagementTab.DAY_PLAN ->
            Button(
                onClick = onFinalizePlan,
                modifier = modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    if (runtimeUiState.runtimeState.dayPlanFinalizedAt != null) {
                        "План дня зафіксовано"
                    } else {
                        "План дня готовий"
                    },
                )
            }
        DayManagementTab.JOURNAL ->
            Button(
                onClick = onStartImplementation,
                modifier = modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (hasOpenDay) "Почати реалізацію" else "Стартувати день і реалізацію")
            }
        DayManagementTab.FINALIZATION ->
            Button(
                onClick = onStartFinalization,
                modifier = modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (hasOpenDay) "Почати фіналізацію" else "Стартувати день і фіналізацію")
            }
        else -> {
            Text(
                text = "",
                modifier = modifier,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
