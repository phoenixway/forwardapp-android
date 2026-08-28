package com.romankozak.forwardappmobile.features.activitytracker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityDescriptor
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityStartLinkSelector

@Composable
fun ActivityInputActionsRow(
    text: String,
    selectedEntityLinks: List<ActivityEntityLink>,
    entityOptions: List<ActivityEntityDescriptor>,
    onEntityLinksChanged: (List<ActivityEntityLink>) -> Unit,
    onQuickDoneClick: (String) -> Unit,
    onBackdatedClick: (String) -> Unit,
    onTimelessClick: () -> Unit,
    onDaySummaryClick: (String) -> Unit,
    trailingContent: @Composable (() -> Unit)?,
) {
    val buttonModifier = Modifier.size(40.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityStartLinkSelector(
            selectedLinks = selectedEntityLinks,
            options = entityOptions,
            onLinksChanged = onEntityLinksChanged,
            modifier = buttonModifier,
        )
        ActivityInputActionButton(
            icon = Icons.Default.CheckCircle,
            description = "Подія без тривалості",
            enabled = text.isNotBlank(),
            onClick = { onQuickDoneClick(text) },
        )
        ActivityInputActionButton(
            icon = Icons.Default.MoreTime,
            description = "Додати минулу активність",
            enabled = text.isNotBlank(),
            onClick = { onBackdatedClick(text) },
        )
        ActivityInputActionButton(
            icon = Icons.Default.AddComment,
            description = "Додати коментар",
            enabled = text.isNotBlank(),
            onClick = onTimelessClick,
        )
        ActivityInputActionButton(
            icon = Icons.Default.Summarize,
            description = "Резюме дня",
            enabled = text.isNotBlank(),
            onClick = { onDaySummaryClick(text) },
        )
        trailingContent?.invoke()
    }
}

@Composable
private fun ActivityInputActionButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            ),
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}
