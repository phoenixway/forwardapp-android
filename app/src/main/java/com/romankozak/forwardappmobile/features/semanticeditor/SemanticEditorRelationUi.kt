package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun RelationSectionHeader(title: String, inFlight: Boolean, onAdd: (() -> Unit)?, count: Int? = null,
    actionLabel: String = "Додати") {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .25f)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            count?.let { Text(it.toString(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary) }
            if (onAdd != null) TextButton(onClick = onAdd, enabled = !inFlight) {
                if (actionLabel == "Додати") Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Text(actionLabel)
            } else Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
internal fun RelationRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onRemove: () -> Unit,
    canRemove: Boolean = true,
    onOpen: (() -> Unit)? = null,
    showReadOnly: Boolean = !canRemove,
) {
    var menu by remember(title) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().then(if (onOpen != null) Modifier.clickable(enabled = enabled, onClick = onOpen) else Modifier)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (showReadOnly) Text("Лише перегляд", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onOpen != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (canRemove) Box {
                IconButton(onClick = { menu = true }, enabled = enabled) {
                    Icon(Icons.Default.MoreVert, "Дії зв’язку: $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu && enabled, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Прибрати зв’язок") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                        onClick = { menu = false; onRemove() })
                }
            }
        }
    }
}
