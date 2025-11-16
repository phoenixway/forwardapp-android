package com.romankozak.forwardappmobile.features.projectscreen.components.backlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
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
    onCopyContent: () -> Unit,
    onRemindersClick: () -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
    ) {
        Column {

            ListItem(
                headlineContent = { Text("Copy content") },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy content") },
                modifier = Modifier.clickable { onCopyContent(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Reminders") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                modifier = Modifier.clickable { onRemindersClick(); onDismiss() }
            )
        }
    }
}
