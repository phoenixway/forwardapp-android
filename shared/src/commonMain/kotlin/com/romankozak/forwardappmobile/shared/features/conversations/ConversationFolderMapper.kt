package com.romankozak.forwardappmobile.shared.features.conversations

import com.romankozak.forwardappmobile.shared.data.database.models.ConversationFolder
import com.romankozak.forwardappmobile.shared.database.ConversationFolders

fun ConversationFolders.toDomain(): ConversationFolder {
    return ConversationFolder(
        id = id,
        name = name
    )
}