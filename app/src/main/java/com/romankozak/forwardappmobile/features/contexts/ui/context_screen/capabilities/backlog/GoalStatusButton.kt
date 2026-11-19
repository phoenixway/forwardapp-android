package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.IndeterminateCheckBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues

@Composable
internal fun GoalStatusButton(
    status: String,
    completed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onStatusChange: (String) -> Unit,
) {
    val normalizedStatus = GoalStatusValues.normalize(status)
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier.size(width = 40.dp, height = 48.dp).combinedClickable(
                role = Role.Button,
                onClickLabel = if (completed) "Зробити активною" else "Позначити виконаною",
                onLongClickLabel = "Вибрати статус",
                onClick = { onCheckedChange(!completed) },
                onLongClick = { menuExpanded = true },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = goalStatusIcon(normalizedStatus),
                contentDescription = "Статус: ${GoalStatusValues.getDisplayName(normalizedStatus)}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            GoalStatusValues.all.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(GoalStatusValues.getDisplayName(candidate)) },
                    leadingIcon = { Icon(goalStatusIcon(candidate), contentDescription = null) },
                    trailingIcon = {
                        if (candidate == normalizedStatus) {
                            Icon(Icons.Default.Check, contentDescription = "Поточний статус")
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        if (candidate != normalizedStatus) onStatusChange(candidate)
                    },
                )
            }
        }
    }
}

private fun goalStatusIcon(status: String): ImageVector = when (status) {
    GoalStatusValues.IN_WORK -> Icons.Outlined.IndeterminateCheckBox
    GoalStatusValues.PAUSED -> Icons.Default.Pause
    GoalStatusValues.UNSURE -> Icons.Default.HelpOutline
    GoalStatusValues.DONE -> Icons.Outlined.CheckBox
    GoalStatusValues.CANCELED -> Icons.Default.Cancel
    else -> Icons.Outlined.CheckBoxOutlineBlank
}
