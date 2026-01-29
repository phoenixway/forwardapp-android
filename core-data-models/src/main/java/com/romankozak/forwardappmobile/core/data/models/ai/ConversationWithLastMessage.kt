package com.romankozak.forwardappmobile.core.data.models.ai

import androidx.room.Embedded
import androidx.room.Relation

data class ConversationWithLastMessage(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId",
    )
    val lastMessage: ChatMessageEntity?,
)
