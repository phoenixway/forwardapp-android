package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacklogItemActionsBottomSheet(
    onDismiss: () -> Unit,
    onCopyContent: () -> Unit,
    onRemindersClick: () -> Unit,
    onDeleteEverywhere: () -> Unit,
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
                modifier = androidx.compose.ui.Modifier.clickable { onCopyContent(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Reminders") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                modifier = androidx.compose.ui.Modifier.clickable { onRemindersClick(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Delete everywhere") },
                leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = "Delete everywhere") },
                modifier = androidx.compose.ui.Modifier.clickable { onDeleteEverywhere(); onDismiss() }
            )
        }
    }
}
