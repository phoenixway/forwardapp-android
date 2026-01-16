package com.romankozak.forwardappmobile.features.ai.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ConversationDrawer(
    drawerItems: List<DrawerItem>,
    onConversationClick: (Long) -> Unit
) {
    Column {
        Text(
            "Chats",
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium
        )
        Divider()
        val expandedFolders = remember { mutableStateMapOf<Long, Boolean>() }

        LazyColumn {
            items(drawerItems) { item ->
                when (item) {
                    is DrawerItem.Folder -> {
                        val isExpanded = expandedFolders[item.folder.id] ?: false
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedFolders[item.folder.id] = !isExpanded }
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    item.folder.name,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                                )
                            }
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    item.conversations.forEach { conversation ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    conversation.conversation.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier
                                                .clickable { onConversationClick(conversation.conversation.id) }
                                                .padding(start = 32.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is DrawerItem.Conversation -> {
                        ListItem(
                            headlineContent = {
                                Text(
                                    item.conversationWithLastMessage.conversation.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .clickable { onConversationClick(item.conversationWithLastMessage.conversation.id) }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}