package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType

private const val DELETE_DIALOG_SPACING_DP = 8
private const val DELETE_DIALOG_PADDING_VERTICAL_DP = 12
private const val DELETE_DIALOG_PADDING_HORIZONTAL_DP = 16
private const val LOADING_INDICATOR_SIZE_DP = 48
private const val LOADING_SPACING_DP = 16

fun CreateConnectionType.toPickerCreateAction(): PickerCreateAction =
    when (this) {
        CreateConnectionType.CONTEXT -> PickerCreateAction.CONTEXT
        CreateConnectionType.NOTE_DOCUMENT -> PickerCreateAction.NOTE
        CreateConnectionType.JOURNAL_DOCUMENT -> PickerCreateAction.JOURNAL_DOCUMENT
        CreateConnectionType.MUSIC_NOTE -> PickerCreateAction.MUSIC_NOTE
        CreateConnectionType.CHECKLIST -> PickerCreateAction.CHECKLIST
        CreateConnectionType.SCRIPT -> PickerCreateAction.NOTE
        CreateConnectionType.EXTERNAL_LINK -> PickerCreateAction.WEB_LINK
        CreateConnectionType.OBSIDIAN_NOTE -> PickerCreateAction.OBSIDIAN
    }

@Composable
fun EditRecurringTaskDialog(
    taskWithReminder: DayTaskWithReminder,
    onDismiss: () -> Unit,
    onConfirmEditSingle: (DayTaskWithReminder) -> Unit,
    onConfirmEditAll: (DayTaskWithReminder) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати повторюване завдання") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DELETE_DIALOG_SPACING_DP.dp)) {
                Text("Це завдання є частиною серії. Виберіть, що саме потрібно змінити.")
                SeriesActionRow(
                    label = "Тільки це завдання",
                    onClick = { onConfirmEditSingle(taskWithReminder) },
                )
                SeriesActionRow(
                    label = "Це та всі наступні завдання",
                    onClick = { onConfirmEditAll(taskWithReminder) },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
fun DeleteRecurringTaskDialog(
    taskWithReminder: DayTaskWithReminder,
    onDismiss: () -> Unit,
    onConfirmDeleteSingle: (DayTaskWithReminder) -> Unit,
    onConfirmDeleteAll: (DayTaskWithReminder) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видалити повторюване завдання?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DELETE_DIALOG_SPACING_DP.dp)) {
                Text("Це завдання є частиною серії. Виберіть, що саме потрібно видалити.")
                SeriesDeleteButton(
                    label = "Тільки це завдання",
                    onClick = { onConfirmDeleteSingle(taskWithReminder) },
                )
                SeriesDeleteButton(
                    label = "Це та всі наступні",
                    onClick = { onConfirmDeleteAll(taskWithReminder) },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(LOADING_INDICATOR_SIZE_DP.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(LOADING_SPACING_DP.dp))
            Text(
                text = "Завантаження плану...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SeriesActionRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(
                    vertical = DELETE_DIALOG_PADDING_VERTICAL_DP.dp,
                    horizontal = DELETE_DIALOG_PADDING_HORIZONTAL_DP.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
    }
}

@Composable
private fun SeriesDeleteButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
    ) {
        Text(label)
    }
}
