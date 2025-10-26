package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
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
    onDelete: () -> Unit,
    onMoveToTop: () -> Unit,
    onAddToDayPlan: () -> Unit,
    onShowGoalTransportMenu: () -> Unit,
    onStartTracking: () -> Unit,
    onCopyContent: () -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
    ) {
        Column {
            ListItem(
                headlineContent = { Text("Delete") },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                modifier = androidx.compose.ui.Modifier.clickable { onDelete(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Move to top") },
                leadingContent = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move to top") },
                modifier = androidx.compose.ui.Modifier.clickable { onMoveToTop(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Add to day plan") },
                leadingContent = { Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to day plan") },
                modifier = androidx.compose.ui.Modifier.clickable { onAddToDayPlan(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Transport goal") },
                leadingContent = { Icon(Icons.Default.Send, contentDescription = "Transport goal") },
                modifier = androidx.compose.ui.Modifier.clickable { onShowGoalTransportMenu(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Start tracking") },
                leadingContent = { Icon(Icons.Default.Timer, contentDescription = "Start tracking") },
                modifier = androidx.compose.ui.Modifier.clickable { onStartTracking(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Copy content") },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy content") },
                modifier = androidx.compose.ui.Modifier.clickable { onCopyContent(); onDismiss() }
            )
        }
    }
}