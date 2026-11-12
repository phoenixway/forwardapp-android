package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction

/** The former Arc FAB actions, without the duplicate canonical source-picker entry. */
internal fun strategicArcMenuActions(
    ready: Boolean,
    onBeacon: () -> Unit,
    onGroup: () -> Unit,
    onArtifact: () -> Unit,
    onLinks: () -> Unit,
) = listOf(
    ContextualAction("arc-beacon", "Орієнтир як ArcQuest", Icons.Outlined.Add, enabled = ready, onClick = onBeacon),
    ContextualAction("arc-group", "Група орієнтирів як ArcQuest", Icons.Outlined.Add, enabled = ready, onClick = onGroup),
    ContextualAction("arc-open-artifact", "Відкрити артефакт", Icons.AutoMirrored.Outlined.OpenInNew, enabled = ready, onClick = onArtifact),
    ContextualAction("arc-links", "Показати зв’язки", Icons.Outlined.Link, enabled = ready, onClick = onLinks),
)
