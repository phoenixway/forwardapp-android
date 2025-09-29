package com.romankozak.forwardappmobile.ui.screens.attachments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.attachments.AttachmentType

@Composable
fun AddAttachmentDialog(
    onDismiss: () -> Unit,
    onAttachmentTypeSelected: (AttachmentType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add attachment") },
        text = {
            Column {
                Text("Note", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.NOTE) }.padding(16.dp))
                Text("Custom List", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.CUSTOM_LIST) }.padding(16.dp))
                Text("Web Link", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.WEB_LINK) }.padding(16.dp))
                Text("Obsidian Link", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.OBSIDIAN_LINK) }.padding(16.dp))
                Text("Link to another list", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.LIST_LINK) }.padding(16.dp))
                Text("Shortcut to another list", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.SHORTCUT) }.padding(16.dp))
            }
        },
        confirmButton = { }
    )
}