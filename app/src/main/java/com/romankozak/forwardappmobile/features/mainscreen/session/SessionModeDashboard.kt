package com.romankozak.forwardappmobile.features.mainscreen.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

@Composable
fun SessionModeDashboard(
    state: SessionModeState,
    latestSessionReason: String?,
    onModeSelected: (SessionMode) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenModeContext: (String) -> Unit,
) {
    val currentMode = state.mode
    val isDarkTheme = isSystemInDarkTheme()
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val activeDuration =
        remember(state.startedAt) {
            state.startedAt?.let(::formatElapsedDuration)
        }
    val headerContainerColor =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionModeCard(
            currentMode = currentMode,
            latestSessionReason = latestSessionReason,
            activeDuration = activeDuration,
            isExpanded = isExpanded,
            headerContainerColor = headerContainerColor,
            modeMenuExpanded = modeMenuExpanded,
            onExpandedChange = onExpandedChange,
            onModeMenuExpandedChange = { modeMenuExpanded = it },
            onModeSelected = { mode ->
                modeMenuExpanded = false
                onModeSelected(mode)
            },
            onOpenModeContext = onOpenModeContext,
        )
    }
}

@Composable
private fun SessionModeCard(
    currentMode: SessionMode,
    latestSessionReason: String?,
    activeDuration: String?,
    isExpanded: Boolean,
    headerContainerColor: Color,
    modeMenuExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModeMenuExpandedChange: (Boolean) -> Unit,
    onModeSelected: (SessionMode) -> Unit,
    onOpenModeContext: (String) -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val accentColor = modeAccentColor(currentMode)
    val activeCardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
    val reasonContainerColor =
        if (isDarkTheme) {
            accentColor.copy(alpha = 0.34f)
        } else {
            accentColor.copy(alpha = 0.18f)
        }

    Card(colors = CardDefaults.cardColors(containerColor = activeCardColor)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Режим сесії",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                        modifier = Modifier.clickable { onModeMenuExpandedChange(true) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = currentMode.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                imageVector = Icons.Outlined.SwapVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    activeDuration?.let {
                        Text(
                            text = "Активний вже $it",
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                IconButton(onClick = { onExpandedChange(!isExpanded) }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = modeMenuExpanded,
                onDismissRequest = { onModeMenuExpandedChange(false) },
            ) {
                SessionMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.title) },
                        onClick = { onModeSelected(mode) },
                    )
                }
            }

            if (!isExpanded) {
                return@Column
            }

            latestSessionReason?.let { reason ->
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = reasonContainerColor,
                        ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Остання причина переходу",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (currentMode == SessionMode.UNSET) {
                Text(
                    text = "Зараз режим не задано",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                currentMode.systemContextId?.let { contextId ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.Button(onClick = { onOpenModeContext(contextId) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Text("Контекст", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { onModeSelected(SessionMode.UNSET) }) {
                            Icon(Icons.Outlined.StopCircle, contentDescription = null)
                            Text("Завершити", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatElapsedDuration(startedAt: Long): String {
    val elapsedMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60
    return buildString {
        if (hours > 0) append("${hours}г ")
        append("${minutes}хв")
    }
}

private fun modeAccentColor(mode: SessionMode): Color =
    when (mode) {
        SessionMode.IMPROVE -> Color(0xFF2E7D32)
        SessionMode.EXECUTION -> Color(0xFF1565C0)
        SessionMode.CONTROL -> Color(0xFFEF6C00)
        SessionMode.RECOVERY -> Color(0xFF00838F)
        SessionMode.EMERGENCY -> Color(0xFFB71C1C)
        SessionMode.UNSET -> Color(0xFF616161)
    }
