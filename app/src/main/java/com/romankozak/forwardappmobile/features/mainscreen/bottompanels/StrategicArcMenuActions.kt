package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction

/** The former Arc FAB actions, without the duplicate canonical source-picker entry. */
internal fun strategicArcMenuActions(
    ready: Boolean,
    hasPrevious: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBeacon: () -> Unit,
    onGroup: () -> Unit,
    onArtifact: () -> Unit,
    onLinks: () -> Unit,
) = listOf(
    ContextualAction("arc-previous", "Попередня арка", Icons.AutoMirrored.Filled.ArrowBack,
        enabled = ready && hasPrevious, onClick = onPrevious),
    ContextualAction("arc-next", "Наступна арка", Icons.AutoMirrored.Filled.ArrowForward, enabled = ready, onClick = onNext),
    ContextualAction("arc-beacon", "Орієнтир як ArcQuest", Icons.Outlined.Add, enabled = ready, onClick = onBeacon),
    ContextualAction("arc-group", "Група орієнтирів як ArcQuest", Icons.Outlined.Add, enabled = ready, onClick = onGroup),
    ContextualAction("arc-open-artifact", "Відкрити артефакт", Icons.AutoMirrored.Outlined.OpenInNew, enabled = ready, onClick = onArtifact),
    ContextualAction("arc-links", "Показати зв’язки", Icons.Outlined.Link, enabled = ready, onClick = onLinks),
)
