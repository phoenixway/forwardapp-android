package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Scroll gestures scroll actions; they must not also change the active input mode. */
@Composable
internal fun ModeIconDock(
    currentMode: OmniboxMode,
    palette: OmniboxModePalette,
    onShowCreate: () -> Unit,
    onShowRecents: () -> Unit,
    recentsSelected: Boolean,
    onSetMode: (OmniboxMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = palette.panelChrome.copy(alpha = .92f),
        border = BorderStroke(1.dp, palette.panelOutline.copy(alpha = .08f))) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OmniboxMode.entries.forEach { mode ->
                ModeIconButton(mode, selected = !recentsSelected && mode == currentMode,
                    palette = palette, onClick = { onSetMode(mode) })
                if (mode == OmniboxMode.DataSearch) {
                    ModeDockActionButton(Icons.Default.History, "Недавні", recentsSelected, palette, onShowRecents)
                    ModeDockActionButton(Icons.Default.Add, "Створити", false, palette, onShowCreate)
                }
            }
        }
    }
}

@Composable
private fun ModeIconButton(
    mode: OmniboxMode,
    selected: Boolean,
    palette: OmniboxModePalette,
    onClick: () -> Unit,
) {
    ModeDockActionButton(
        imageVector = modeIcon(mode),
        contentDescription = modeTitle(mode),
        selected = selected,
        palette = palette,
        onClick = onClick,
    )
}

@Composable
private fun ModeDockActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    selected: Boolean,
    palette: OmniboxModePalette,
    onClick: () -> Unit,
) {
    val containerColor =
        if (selected) palette.modeIconContainer else Color.Transparent
    val borderColor =
        if (selected) palette.iconTint.copy(alpha = 0.18f) else Color.Transparent
    val iconColor =
        if (selected) palette.iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = iconColor,
            )
        }
    }
}

private fun modeIcon(mode: OmniboxMode): ImageVector =
    when (mode) {
        OmniboxMode.DataSearch -> Icons.Default.Search
        OmniboxMode.Command -> Icons.Default.Terminal
        OmniboxMode.QuickCatchInbox -> Icons.Outlined.MoveToInbox
        OmniboxMode.StartActivity -> Icons.Default.PlayArrow
        OmniboxMode.AddActivityEvent -> Icons.Default.Event
    }

private fun modeTitle(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Пошук даних"
        OmniboxMode.Command -> "Команди"
        OmniboxMode.QuickCatchInbox -> "Quick catch to inbox"
        OmniboxMode.StartActivity -> "Start record activity"
        OmniboxMode.AddActivityEvent -> "Add tracker event"
    }
