package com.romankozak.forwardappmobile.ui.screens.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.ConversationFolderEntity

@Composable
fun ConversationDrawer(
    conversations: List<ConversationWithLastMessage>,
    folders: List<ConversationFolderEntity>,
    onConversationClick: (Long) -> Unit,
    viewModel: ChatViewModel
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = {
                viewModel.createFolder(it)
                showNewFolderDialog = false
            }
        )
    }

    Column {
        Text("Chats", modifier = Modifier.padding(16.dp))
        Divider()
        Button(onClick = { showNewFolderDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = "New folder")
            Text("New Folder")
        }
        LazyColumn {
            items(folders) { folder ->
                Text(folder.name, modifier = Modifier.padding(16.dp))
            }
            items(conversations) { conversation ->
                ListItem(
                    headlineContent = { Text(conversation.conversation.title.take(30)) },
                    supportingContent = { Text((conversation.lastMessage?.text ?: "").take(40)) },
                    modifier = Modifier.clickable { onConversationClick(conversation.conversation.id) }
                )
            }
        }
    }
}

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column {
            Text("New Folder")
            OutlinedTextField(value = folderName, onValueChange = { folderName = it })
            Button(onClick = { onConfirm(folderName) }) {
                Text("Create")
            }
        }
    }
}
