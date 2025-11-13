package com.romankozak.forwardappmobile.shared.features.aichat.data.mappers

import com.romankozak.forwardappmobile.shared.features.aichat.ConversationFolders
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationFolder

fun ConversationFolders.toDomain(): ConversationFolder =
    ConversationFolder(
        id = id,
        name = name,
    )
