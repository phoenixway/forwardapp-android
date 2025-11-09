package com.romankozak.forwardappmobile.shared.features.conversations

import com.romankozak.forwardappmobile.shared.data.database.models.ConversationFolder
import com.romankozak.forwardappmobile.shared.database.Conversation_folders

fun Conversation_folders.toDomain(): ConversationFolder {
    return ConversationFolder(
        id = this.id,
        name = this.name
    )
}
