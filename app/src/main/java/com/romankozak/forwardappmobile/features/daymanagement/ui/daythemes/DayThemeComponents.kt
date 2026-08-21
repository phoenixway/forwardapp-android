package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val dayThemeColors =
    listOf(
        0xFFE53935, 0xFFFF7A45, 0xFFFFC107, 0xFF34C759,
        0xFF2F80ED, 0xFF7C4DFF, 0xFFD946EF, 0xFF607D8B,
    )

internal val dayThemeIconKeys =
    listOf("target", "sparkles", "heart", "brain", "work", "home", "activity", "leaf")

@Composable
internal fun dayThemeIcon(key: String): ImageVector =
    when (key) {
        "heart" -> Icons.Outlined.FavoriteBorder
        "home" -> Icons.Outlined.Home
        "sparkles", "spark" -> Icons.Outlined.AutoAwesome
        "brain", "mind" -> Icons.Outlined.Psychology
        "work" -> Icons.Outlined.WorkOutline
        "activity" -> Icons.Outlined.DirectionsRun
        "leaf" -> Icons.Outlined.Eco
        "flag" -> Icons.Outlined.Flag
        else -> Icons.Outlined.TrackChanges
    }

internal fun themeColor(theme: DayTheme): Color = dayThemeColor(theme.colorArgb)

internal fun dayThemeColor(argb: Long): Color = Color(argb.toInt())

@Composable
fun DayThemeChip(
    theme: DayTheme,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val color = themeColor(theme)
    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = color.copy(alpha = if (selected) 0.20f else 0.11f),
        contentColor = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (selected) 0.72f else 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(dayThemeIcon(theme.iconKey), contentDescription = null, modifier = Modifier.size(13.dp))
            Text(
                text = theme.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            suffix?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EntityThemeSelector(
    themes: List<DayTheme>,
    selectedThemeIds: Set<String>,
    entityLabel: String,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val activeThemes = themes.filter(DayTheme::isActive)
    val selectedThemes = activeThemes.filter { it.id in selectedThemeIds }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        selectedThemes.take(3).forEach { theme -> DayThemeChip(theme = theme) }
        if (selectedThemes.size > 3) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "+${selectedThemes.size - 3}",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        IconButton(onClick = { sheetVisible = true }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Sell, contentDescription = "Призначити теми", modifier = Modifier.size(17.dp))
        }
    }

    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { sheetVisible = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Text(
                text = "Теми · $entityLabel",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (activeThemes.isEmpty()) {
                Text(
                    text = "Спочатку створіть тему у підвкладці «Теми».",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(activeThemes, key = DayTheme::id) { theme ->
                        val checked = theme.id in selectedThemeIds
                        val supportingContent: (@Composable () -> Unit)? =
                            theme.comment.takeIf(String::isNotBlank)?.let { comment ->
                                {
                                    Text(
                                        text = comment,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        ListItem(
                            headlineContent = { Text(theme.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = supportingContent,
                            leadingContent = {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(shape = CircleShape, color = themeColor(theme), modifier = Modifier.size(30.dp)) {}
                                    Icon(dayThemeIcon(theme.iconKey), null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            },
                            trailingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle(theme.id) }) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayThemeUsageSummary(
    label: String,
    themes: List<DayTheme>,
    entityIds: List<String>,
    assignments: Map<String, Set<String>>,
    percentByEntity: Map<String, Int> = emptyMap(),
    minutesByEntity: Map<String, Long> = emptyMap(),
    predictedDayDurationMinutes: Long? = null,
    modifier: Modifier = Modifier,
) {
    val used = themes.filter(DayTheme::isActive).mapNotNull { theme ->
        val matchingIds = entityIds.filter { theme.id in assignments[it].orEmpty() }
        theme.takeIf { matchingIds.isNotEmpty() }?.let { it to matchingIds }
    }
    if (used.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        used.forEach { (theme, ids) ->
            val percent = ids.sumOf { percentByEntity[it] ?: 0 }
            val minutes =
                ids.sumOf { minutesByEntity[it] ?: 0L }.takeIf { it > 0 }
                    ?: predictedDayDurationMinutes?.times(percent)?.div(100)?.takeIf { it > 0 }
            val suffix = buildString {
                append(ids.size)
                if (percent > 0) append(" · $percent%")
                if (minutes != null) append(" · ${formatThemeMinutes(minutes)}")
            }
            DayThemeChip(theme = theme, suffix = suffix)
        }
    }
}

private fun formatThemeMinutes(minutes: Long): String =
    if (minutes < 60) "${minutes}хв" else {
        val hours = minutes / 60f
        val formatted = if (minutes % 60L == 0L) hours.toInt().toString() else String.format("%.1f", hours)
        "${formatted}г"
    }
