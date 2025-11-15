package com.romankozak.forwardappmobile.shared.features.aichat.data.mappers

import com.romankozak.forwardappmobile.shared.features.aichat.ChatMessages
import com.romankozak.forwardappmobile.shared.features.aichat.Conversations
import com.romankozak.forwardappmobile.shared.features.aichat.GetConversationsWithLastMessage
import com.romankozak.forwardappmobile.shared.features.aichat.GetConversationsWithLastMessageByFolder
import com.romankozak.forwardappmobile.shared.features.aichat.GetConversationsWithLastMessageWithoutFolder
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ChatMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.Conversation
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationWithLastMessage

fun Conversations.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        creationTimestamp = creationTimestamp,
        folderId = folderId,
    )

fun ChatMessages.toDomain(): ChatMessage =
    ChatMessage(
        id = id,
        conversationId = conversationId,
        text = text,
        isFromUser = isFromUser,
        isError = isError,
        timestamp = timestamp,
        isStreaming = isStreaming,
    )

fun GetConversationsWithLastMessage.toDomain(): ConversationWithLastMessage =
    ConversationWithLastMessage(
        conversation = Conversation(
            id = id,
            title = title,
            creationTimestamp = creationTimestamp,
            folderId = folderId,
        ),
        lastMessage = lastMessageId?.let {
            ChatMessage(
                id = lastMessageId,
                conversationId = id,
                text = lastMessageText ?: "",
                isFromUser = lastMessageIsFromUser ?: false,
                isError = lastMessageIsError ?: false,
                timestamp = lastMessageTimestamp ?: 0L,
                isStreaming = lastMessageIsStreaming ?: false,
            )
        },
    )

fun GetConversationsWithLastMessageByFolder.toDomain(): ConversationWithLastMessage =
    ConversationWithLastMessage(
        conversation = Conversation(
            id = id,
            title = title,
            creationTimestamp = creationTimestamp,
            folderId = folderId,
        ),
        lastMessage = lastMessageId?.let {
            ChatMessage(
                id = lastMessageId,
                conversationId = id,
                text = lastMessageText ?: "",
                isFromUser = lastMessageIsFromUser ?: false,
                isError = lastMessageIsError ?: false,
                timestamp = lastMessageTimestamp ?: 0L,
                isStreaming = lastMessageIsStreaming ?: false,
            )
        },
    )

fun GetConversationsWithLastMessageWithoutFolder.toDomain(): ConversationWithLastMessage =
    ConversationWithLastMessage(
        conversation = Conversation(
            id = id,
            title = title,
            creationTimestamp = creationTimestamp,
            folderId = folderId,
        ),
        lastMessage = lastMessageId?.let {
            ChatMessage(
                id = lastMessageId,
                conversationId = id,
                text = lastMessageText ?: "",
                isFromUser = lastMessageIsFromUser ?: false,
                isError = lastMessageIsError ?: false,
                timestamp = lastMessageTimestamp ?: 0L,
                isStreaming = lastMessageIsStreaming ?: false,
            )
        },
    )
