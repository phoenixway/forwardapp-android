package com.romankozak.forwardappmobile.features.userawareness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import com.romankozak.forwardappmobile.domain.userawareness.UserStateInterval

private const val DEFAULT_CRISIS_LEVEL = 1
private const val MAX_CRISIS_LEVEL = 3

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
            UserAwarenessStateType.CRISIS -> "CRISIS L${state.crisisLevel ?: DEFAULT_CRISIS_LEVEL}"
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
fun UserAwarenessHeaderBadge(
    activeState: UserStateInterval?,
    onOpenQuickSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = activeState ?: return
    if (state.type == UserAwarenessStateType.NORMAL) return

    val (label, container, content) =
        when (state.type) {
            UserAwarenessStateType.CRISIS ->
                Triple(
                    "C${state.crisisLevel ?: DEFAULT_CRISIS_LEVEL}",
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
            UserAwarenessStateType.EXHAUSTION ->
                Triple(
                    "EXH",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
            UserAwarenessStateType.UNPRODUCTIVE ->
                Triple(
                    "LOW",
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
            UserAwarenessStateType.NORMAL -> Triple("", Color.Transparent, Color.Transparent)
        }

    Surface(
        onClick = onOpenQuickSwitch,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun UserAwarenessQuickSwitchDialog(
    onDismiss: () -> Unit,
    onNormal: () -> Unit,
    onExhaustion: () -> Unit,
    onUnproductive: () -> Unit,
    onCrisis: (level: Int, label: String?) -> Unit,
) {
    var crisisLevel by remember { mutableIntStateOf(DEFAULT_CRISIS_LEVEL) }
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
                CrisisLevelSelector(
                    crisisLevel = crisisLevel,
                    onLevelSelected = { crisisLevel = it },
                )
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

@Composable
private fun CrisisLevelSelector(
    crisisLevel: Int,
    onLevelSelected: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (level in DEFAULT_CRISIS_LEVEL..MAX_CRISIS_LEVEL) {
            FilterChip(
                selected = crisisLevel == level,
                onClick = { onLevelSelected(level) },
                label = { Text("L$level") },
            )
        }
    }
}
