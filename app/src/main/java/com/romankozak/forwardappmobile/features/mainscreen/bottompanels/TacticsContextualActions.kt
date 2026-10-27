package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode

internal fun tacticsComposerActions(
    selected: TacticsWorkspaceMode,
    canPaste: Boolean,
    execution: Boolean,
    onMode: (TacticsWorkspaceMode) -> Unit,
    onStreams: () -> Unit,
    onPaste: () -> Unit,
    onSource: () -> Unit,
    onLinks: () -> Unit,
): List<ContextualAction> = buildList {
    if (!execution) {
        add(ContextualAction("mission-source", "Створити Mission на основі сутності", Icons.Default.Add, onClick = onSource))
        add(ContextualAction("scope-links", "Показати зв’язки", Icons.Outlined.Link, onClick = onLinks))
        add(ContextualAction("paste", "Вставити в потік", Icons.Outlined.ContentPaste, enabled = canPaste, onClick = onPaste))
    }
    add(ContextualAction("streams-settings", "Керувати потоками", Icons.Outlined.Settings, onClick = onStreams))
    TacticsWorkspaceMode.entries.filter { !execution || it != TacticsWorkspaceMode.PLAN }.forEach { mode ->
        add(ContextualAction("tactics-mode-${mode.name}", when (mode) {
            TacticsWorkspaceMode.STREAMS -> "Потоки"
            TacticsWorkspaceMode.ALL -> "Усі місії"
            TacticsWorkspaceMode.PLAN -> "План тижня"
        }, selected = selected == mode, onClick = { onMode(mode) }))
    }
}
