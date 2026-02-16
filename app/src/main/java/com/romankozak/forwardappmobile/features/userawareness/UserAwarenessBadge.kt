package com.romankozak.forwardappmobile.features.userawareness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import com.romankozak.forwardappmobile.domain.userawareness.UserStateInterval

@Composable
fun UserAwarenessBadge(
    activeState: UserStateInterval?,
    onOpenQuickSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = activeState ?: return
    val label =
        when (state.type) {
            UserAwarenessStateType.NORMAL -> "NORMAL"
            UserAwarenessStateType.CRISIS -> "CRISIS L${state.crisisLevel ?: 1}"
            UserAwarenessStateType.EXHAUSTION -> "EXHAUSTION"
            UserAwarenessStateType.UNPRODUCTIVE -> "LOW DRIVE"
        }
    AssistChip(
        onClick = onOpenQuickSwitch,
        label = { Text(label) },
        modifier = modifier,
    )
}

@Composable
fun UserAwarenessQuickSwitchDialog(
    onDismiss: () -> Unit,
    onNormal: () -> Unit,
    onExhaustion: () -> Unit,
    onUnproductive: () -> Unit,
    onCrisis: (level: Int, label: String?) -> Unit,
) {
    var crisisLevel by remember { mutableIntStateOf(1) }
    var crisisLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Awareness State") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            onNormal()
                            onDismiss()
                        },
                        label = { Text("Normal") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onExhaustion()
                            onDismiss()
                        },
                        label = { Text("Exhaustion") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onUnproductive()
                            onDismiss()
                        },
                        label = { Text("Need a push") },
                    )
                }

                Text("Crisis level", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { level ->
                        FilterChip(
                            selected = crisisLevel == level,
                            onClick = { crisisLevel = level },
                            label = { Text("L$level") },
                        )
                    }
                }
                OutlinedTextField(
                    value = crisisLabel,
                    onValueChange = { crisisLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Label (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCrisis(crisisLevel, crisisLabel.trim().ifBlank { null })
                    onDismiss()
                },
            ) {
                Text("Set crisis")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
