package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
