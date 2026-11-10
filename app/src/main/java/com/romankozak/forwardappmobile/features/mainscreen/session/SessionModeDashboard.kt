@file:Suppress("MagicNumber")

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
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.context.SystemContexts
import java.util.concurrent.TimeUnit

private val ImproveModeColor = Color(0xFF2E7D32)
private val ExecutionModeColor = Color(0xFF1565C0)
private val ControlModeColor = Color(0xFFEF6C00)
private val RecoveryModeColor = Color(0xFF00838F)
private val EmergencyModeColor = Color(0xFFB71C1C)
private val UnsetModeColor = Color(0xFF616161)

private const val HeaderContainerAlpha = 0.42f
private const val ActiveCardAlpha = 0.08f
private const val DarkReasonContainerAlpha = 0.34f
private const val LightReasonContainerAlpha = 0.18f

@Composable
fun SessionModeDashboard(
    state: SessionModeState,
    latestSessionReason: String?,
    onModeSelected: (SessionMode) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenModeContext: (String) -> Unit,
    showHeader: Boolean = true,
) {
    val currentMode = state.mode
    val isDarkTheme = isSystemInDarkTheme()
    var modeMenuExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val activeDuration =
        remember(state.startedAt) {
            state.startedAt?.let(::formatElapsedDuration)
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionModeCard(
            showHeader = showHeader,
            currentMode = currentMode,
            latestSessionReason = latestSessionReason,
            activeDuration = activeDuration,
            isExpanded = isExpanded,
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
    showHeader: Boolean,
    currentMode: SessionMode,
    latestSessionReason: String?,
    activeDuration: String?,
    isExpanded: Boolean,
    modeMenuExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModeMenuExpandedChange: (Boolean) -> Unit,
    onModeSelected: (SessionMode) -> Unit,
    onOpenModeContext: (String) -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val accentColor = modeAccentColor(currentMode)
    val activeCardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ActiveCardAlpha)
    val reasonContainerColor =
        if (isDarkTheme) {
            accentColor.copy(alpha = DarkReasonContainerAlpha)
        } else {
            accentColor.copy(alpha = LightReasonContainerAlpha)
        }

    Card(colors = CardDefaults.cardColors(containerColor = activeCardColor)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (isExpanded) 16.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showHeader) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.deck_session), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (currentMode == SessionMode.UNSET) stringResource(R.string.deck_unset) else currentMode.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    androidx.compose.material3.TextButton(onClick = { onModeMenuExpandedChange(true) }) {
                        Text(stringResource(if (currentMode == SessionMode.UNSET) R.string.deck_choose else R.string.deck_change))
                    }
                    IconButton(onClick = { onExpandedChange(!isExpanded) }) {
                        Icon(if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.deck_details),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = modeMenuExpanded, onDismissRequest = { onModeMenuExpandedChange(false) }) {
                        SessionMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(if (mode == SessionMode.UNSET) stringResource(R.string.deck_unset) else mode.title) },
                                onClick = { onModeSelected(mode) },
                            )
                        }
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SessionMode.entries.forEach { mode ->
                        androidx.compose.material3.FilterChip(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) },
                            label = { Text(if (mode == SessionMode.UNSET) stringResource(R.string.deck_mode_unset) else mode.title) },
                        )
                    }
                }
            }
            if (!isExpanded) return@Column
            activeDuration?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { onOpenModeContext(SystemContexts.ABOUT_MODES.raw) }) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                        Text("About modes", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            } else {
                currentMode.systemContextId?.let { contextId ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { onOpenModeContext(SystemContexts.ABOUT_MODES.raw) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Text("About modes", modifier = Modifier.padding(start = 8.dp))
                        }
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
        SessionMode.IMPROVE -> ImproveModeColor
        SessionMode.EXECUTION -> ExecutionModeColor
        SessionMode.CONTROL -> ControlModeColor
        SessionMode.RECOVERY -> RecoveryModeColor
        SessionMode.EMERGENCY -> EmergencyModeColor
        SessionMode.UNSET -> UnsetModeColor
    }
