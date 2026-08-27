package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchHistoryHintStrip(
    history: List<String>,
    containerColor: Color,
    onHistoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MagicBoxHistoryHintFlow(
        title = "Останні запити",
        items =
            history.map { query ->
                HistoryHintItem(
                    label = query,
                    icon = Icons.Default.History,
                    onClick = { onHistoryClick(query) },
                )
            },
        containerColor = containerColor,
        modifier = modifier,
    )
}

@Composable
internal fun CommandHistoryHintStrip(
    commands: List<OmniboxCommandId>,
    containerColor: Color,
    onCommandClick: (OmniboxCommandId) -> Unit,
    modifier: Modifier = Modifier,
) {
    MagicBoxHistoryHintFlow(
        title = "Активовані команди",
        items =
            commands.map { commandId ->
                HistoryHintItem(
                    label = commandTitle(commandId),
                    icon = commandIcon(commandId),
                    onClick = { onCommandClick(commandId) },
                )
            },
        containerColor = containerColor,
        modifier = modifier,
    )
}

private data class HistoryHintItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MagicBoxHistoryHintFlow(
    title: String,
    items: List<HistoryHintItem>,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val maxHistoryHeight = configuration.screenHeightDp.dp * 0.58f
    val maxChipWidth = (configuration.screenWidthDp - 32).coerceAtLeast(160).dp

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
        )
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHistoryHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items.forEach { item ->
                Surface(
                    onClick = item.onClick,
                    modifier = Modifier.widthIn(max = maxChipWidth),
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor.copy(alpha = 0.58f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                        )
                        Text(
                            text = item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }
    }
}
