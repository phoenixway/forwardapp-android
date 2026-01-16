package com.romankozak.forwardappmobile.features.attachments.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.contextcreen.components.attachments.AttachmentType

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
                Text("Notes", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.NOTES) }.padding(16.dp))
                Text("Web Link", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.WEB_LINK) }.padding(16.dp))
                Text("Obsidian Link", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.OBSIDIAN_LINK) }.padding(16.dp))
                Text("Link to another project", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.PROJECT_LINK) }.padding(16.dp))
                Text("Shortcut to another project", modifier = Modifier.fillMaxWidth().clickable { onAttachmentTypeSelected(AttachmentType.PROJECT_SHORTCUT) }.padding(16.dp))
            }
        },
        confirmButton = { }
    )
}
