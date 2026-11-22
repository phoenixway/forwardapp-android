package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayThemePickerContent
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayThemesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayFocusItemOptionsSheet(
    item: DayFocusItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dayThemesState: DayThemesUiState,
    onToggleTheme: (String) -> Unit,
) {
    var showThemes by remember(item.id) { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        if (showThemes) {
            Column {
                TextButton(onClick = { showThemes = false }) { Text("Назад до дій") }
                DayThemePickerContent(
                    themes = dayThemesState.themes,
                    selectedThemeIds = dayThemesState.assignments[item.id].orEmpty(),
                    entityLabel = item.title,
                    onToggle = onToggleTheme,
                )
            }
        } else {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                DayFocusItemOption(
                    label = "Редагувати",
                    icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = onEdit,
                )
                DayFocusItemOption(
                    label = "Теми",
                    icon = { Icon(Icons.Outlined.Sell, contentDescription = null) },
                    onClick = { showThemes = true },
                )
                DayFocusItemOption(
                    label = "Видалити",
                    color = MaterialTheme.colorScheme.error,
                    icon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = onDelete,
                )
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun DayFocusItemOption(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, color = color) },
        leadingContent = icon,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
