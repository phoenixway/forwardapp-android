package com.romankozak.forwardappmobile.features.ai.chat

import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationFolderEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationWithLastMessage

sealed class DrawerItem {
    data class Folder(
        val folder: ConversationFolderEntity,
        val conversations: List<ConversationWithLastMessage>,
        val isExpanded: Boolean = false,
    ) : DrawerItem()

    data class Conversation(val conversationWithLastMessage: ConversationWithLastMessage) : DrawerItem()
}
