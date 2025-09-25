package com.romankozak.forwardappmobile.ui.screens.chat

import com.romankozak.forwardappmobile.data.database.models.ConversationFolderEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationWithLastMessage

sealed class DrawerItem {
    data class Folder(
        val folder: ConversationFolderEntity,
        val conversations: List<ConversationWithLastMessage>,
        val isExpanded: Boolean = false
    ) : DrawerItem()

    data class Conversation(val conversationWithLastMessage: ConversationWithLastMessage) : DrawerItem()
}