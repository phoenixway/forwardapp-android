package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacklogItemActionsBottomSheet(
    onDismiss: () -> Unit,
    isCompleted: Boolean,
    onOpenGoalProperties: (() -> Unit)? = null,
    onRemindersClick: () -> Unit,
    onAddToDayPlan: () -> Unit,
    onStartTracking: () -> Unit,
    onMoveOrShare: () -> Unit,
    onToggleCompleted: () -> Unit,
    onCopyContent: () -> Unit,
    onDelete: () -> Unit,
    onDeleteEverywhere: () -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
    ) {
        Column {
            if (onOpenGoalProperties != null) {
                ListItem(
                    headlineContent = { Text("Goal properties") },
                    leadingContent = { Icon(Icons.Default.Tune, contentDescription = "Goal properties") },
                    modifier =
                        Modifier.clickable {
                            onOpenGoalProperties()
                            onDismiss()
                        },
                )
            }
            ListItem(
                headlineContent = { Text("Нагадування") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = "Нагадування") },
                modifier =
                    Modifier.clickable {
                        onRemindersClick()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text("Додати в план дня") },
                leadingContent = { Icon(Icons.Default.AddCircle, contentDescription = "Додати в план дня") },
                modifier =
                    Modifier.clickable {
                        onAddToDayPlan()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text("Почати трекінг") },
                leadingContent = { Icon(Icons.Default.PlayCircleOutline, contentDescription = "Почати трекінг") },
                modifier =
                    Modifier.clickable {
                        onStartTracking()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text("Перемістити або поділитися") },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = "Перемістити або поділитися") },
                modifier =
                    Modifier.clickable {
                        onMoveOrShare()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text(if (isCompleted) "Позначити невиконаним" else "Позначити виконаним") },
                leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = "Позначити виконаним") },
                modifier =
                    Modifier.clickable {
                        onToggleCompleted()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text("Копіювати текст") },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = "Копіювати текст") },
                modifier =
                    Modifier.clickable {
                        onCopyContent()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text("Видалити") },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Видалити") },
                modifier =
                    Modifier.clickable {
                        onDelete()
                        onDismiss()
                    },
            )
            ListItem(
                headlineContent = { Text("Видалити всюди") },
                leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = "Видалити всюди") },
                modifier =
                    Modifier.clickable {
                        onDeleteEverywhere()
                        onDismiss()
                    },
            )
        }
    }
}
