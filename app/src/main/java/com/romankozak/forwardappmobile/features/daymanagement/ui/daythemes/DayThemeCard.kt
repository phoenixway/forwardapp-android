package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayThemeCard(
    theme: DayTheme,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    dragModifier: Modifier,
) {
    var actionsVisible by remember(theme.id) { mutableStateOf(false) }
    val themeColor = themeColor(theme)
    val accent = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        lerp(themeColor, Color.White, 0.35f)
    } else {
        themeColor
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = dragModifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DragIndicator, contentDescription = "Перетягнути тему", tint = muted)
            }
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = if (theme.isActive) 0.18f else 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        dayThemeIcon(theme.iconKey),
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = if (theme.isActive) accent else muted,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onEdit).padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    theme.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (theme.isActive) MaterialTheme.colorScheme.onSurface else muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                theme.comment.takeIf(String::isNotBlank)?.let { comment ->
                    Text(
                        comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (theme.isActive) accent.copy(alpha = 0.13f) else muted.copy(alpha = 0.10f),
                ) {
                    Text(
                        "${theme.budgetPercent}% бюджету",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (theme.isActive) accent else muted,
                    )
                }
            }
            Switch(
                checked = theme.isActive,
                onCheckedChange = onActiveChange,
                modifier = Modifier.semantics { contentDescription = "Активність теми ${theme.title}" },
            )
            IconButton(onClick = { actionsVisible = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Дії з темою", tint = muted)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    if (actionsVisible) {
        ModalBottomSheet(
            onDismissRequest = { actionsVisible = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(theme.title, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium)
                ListItem(
                    headlineContent = { Text("Редагувати") },
                    leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    modifier = Modifier.clickable { actionsVisible = false; onEdit() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { actionsVisible = false; onDelete() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
