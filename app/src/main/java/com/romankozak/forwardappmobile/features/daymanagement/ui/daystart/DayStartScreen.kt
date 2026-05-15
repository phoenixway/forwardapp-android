package com.romankozak.forwardappmobile.features.daymanagement.ui.daystart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import com.romankozak.forwardappmobile.features.daymanagement.ui.NeonTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DayStartScreen(
    runtimeState: DayManagementRuntimeState,
    onWakeUp: () -> Unit,
    onSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        NeonTitle(text = "Day Start")
        RuntimeStatusCard(runtimeState = runtimeState)
        Button(
            onClick = onWakeUp,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = "Проснувся!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        Button(
            onClick = onSleep,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("Пішов спати")
        }
    }
}

@Composable
private fun RuntimeStatusCard(runtimeState: DayManagementRuntimeState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Поточна фаза: ${runtimeState.currentPhase.name}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    runtimeState.wokeAt?.let {
                        "Пробудження: ${formatTimestamp(it)}"
                    } ?: "Пробудження ще не зафіксоване",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text =
                    runtimeState.dayPlanFinalizedAt?.let {
                        "План дня зафіксовано: ${formatTimestamp(it)}"
                    } ?: "План дня ще не зафіксований",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
