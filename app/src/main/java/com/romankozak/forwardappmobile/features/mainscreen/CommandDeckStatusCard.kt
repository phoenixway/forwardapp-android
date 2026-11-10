package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.GeneralStatus
import com.romankozak.forwardappmobile.features.mainscreen.lifemanagement.*
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState

/** Presentation-only switch: preserve the focus implementation and its persisted preference. */
internal const val COMMAND_DECK_SHOW_FOCUS_CONTEXTS = false

@Composable
internal fun CommandDeckStatusCard(
    session: SessionModeState,
    onModeSelected: (SessionMode) -> Unit,
    onOpenInsights: () -> Unit,
    viewModel: LifeManagementStatusPanelViewModel = hiltViewModel(),
) {
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    var showLevels by rememberSaveable { mutableStateOf(false) }
    CommandDeckStatusCardContent(session, statuses, onModeSelected, { showLevels = true }, onOpenInsights)
    if (showLevels) {
        AlertDialog(
            onDismissRequest = { showLevels = false },
            title = { Text(stringResource(R.string.deck_levels)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LifeManagementStatusPanelSection(
                        statuses = statuses,
                        isExpanded = true,
                        onToggleExpanded = { showLevels = false },
                        onSave = viewModel::save,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevels = false }) { Text(stringResource(R.string.deck_close)) }
            },
        )
    }
}

@Composable
internal fun CommandDeckStatusCardContent(
    session: SessionModeState,
    statuses: List<LifeManagementLevelStatus>,
    onModeSelected: (SessionMode) -> Unit,
    onOpenLevels: () -> Unit,
    onOpenInsights: (() -> Unit)? = null,
) {
    var showModes by rememberSaveable { mutableStateOf(false) }
    val blocked = statuses.count { it.generalStatus == GeneralStatus.BLOCKED }
    val defected = statuses.count { it.generalStatus == GeneralStatus.DEFECTED }
    val hasProblems = blocked + defected > 0
    val loaded = statuses.isNotEmpty()
    // Reuse the existing Levels ready palette with contrast for the active app theme.
    val successColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        ReadyDarkContentColor
    } else {
        ReadyLightContentColor
    }
    val levelColor = when {
        hasProblems -> MaterialTheme.colorScheme.error
        loaded -> successColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("deck-status-card"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column {
            Box {
                StatusRow(
                    icon = Icons.Outlined.SwapVert,
                    label = stringResource(R.string.deck_session),
                    value = if (session.mode == SessionMode.UNSET) stringResource(R.string.deck_mode_unset) else session.mode.title,
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { showModes = true },
                    modifier = Modifier.testTag("deck-session-row"),
                )
                DropdownMenu(expanded = showModes, onDismissRequest = { showModes = false }) {
                    SessionMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(if (mode == SessionMode.UNSET) stringResource(R.string.deck_mode_unset) else mode.title) },
                            onClick = { showModes = false; onModeSelected(mode) },
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            StatusRow(
                icon = Icons.Outlined.Layers,
                label = stringResource(R.string.deck_levels_short),
                value = when {
                    hasProblems -> stringResource(R.string.deck_levels_problem, blocked, defected)
                    loaded -> stringResource(R.string.deck_ok)
                    else -> stringResource(R.string.deck_loading)
                },
                valueColor = levelColor,
                success = loaded && !hasProblems,
                onClick = onOpenLevels,
                modifier = Modifier.testTag("deck-levels-row"),
            )
            onOpenInsights?.let { openInsights ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                StatusRow(
                    icon = Icons.Outlined.AutoAwesome,
                    label = stringResource(R.string.deck_ai_insights),
                    value = "",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = openInsights,
                    modifier = Modifier.testTag("deck-insights-row"),
                )
            }

        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    success: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 50.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurface)
        if (success) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = valueColor)
        Text(value, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            color = valueColor, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
