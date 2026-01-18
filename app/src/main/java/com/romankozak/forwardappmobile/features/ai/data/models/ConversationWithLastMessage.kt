package com.romankozak.forwardappmobile.features.ai.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class ConversationWithLastMessage(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val lastMessage: ChatMessageEntity?
)
