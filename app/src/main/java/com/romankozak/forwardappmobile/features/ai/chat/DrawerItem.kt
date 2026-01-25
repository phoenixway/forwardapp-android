package com.romankozak.forwardappmobile.features.ai.chat

import com.romankozak.forwardappmobile.features.ai.data.models.ConversationFolderEntity
import com.romankozak.forwardappmobile.features.ai.data.models.ConversationWithLastMessage

sealed class DrawerItem {
    data class Folder(
        val folder: ConversationFolderEntity,
        val conversations: List<ConversationWithLastMessage>,
        val isExpanded: Boolean = false,
    ) : DrawerItem()

    data class Conversation(val conversationWithLastMessage: ConversationWithLastMessage) : DrawerItem()
}
