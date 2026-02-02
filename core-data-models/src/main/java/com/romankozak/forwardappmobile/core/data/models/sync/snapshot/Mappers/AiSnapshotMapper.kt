package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.mappers

import com.romankozak.forwardappmobile.core.data.models.ai.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai.*

// --- Conversation Mapping ---
fun ConversationEntity.toSnapshot(): ConversationSnapshot = ConversationSnapshot(
    id = this.id,
    title = this.title,
    creationTimestamp = this.creationTimestamp,
    folderId = this.folderId
)

fun ConversationSnapshot.toEntity(): ConversationEntity = ConversationEntity(
    id = this.id,
    title = this.title,
    creationTimestamp = this.creationTimestamp,
    folderId = this.folderId
)

// --- ChatMessage Mapping ---
fun ChatMessageEntity.toSnapshot(): ChatMessageSnapshot = ChatMessageSnapshot(
    id = this.id,
    conversationId = this.conversationId,
    text = this.text,
    isFromUser = this.isFromUser,
    isError = this.isError,
    timestamp = this.timestamp
)

fun ChatMessageSnapshot.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = this.id,
    conversationId = this.conversationId,
    text = this.text,
    isFromUser = this.isFromUser,
    isError = this.isError,
    timestamp = this.timestamp
)

// --- AiInsight Mapping ---
fun AiInsightEntity.toSnapshot(): AiInsightSnapshot = AiInsightSnapshot(
    id = this.id,
    text = this.text,
    type = this.type,
    timestamp = this.timestamp,
    isRead = this.isRead,
    isFavorite = this.isFavorite
)

fun AiInsightSnapshot.toEntity(): AiInsightEntity = AiInsightEntity(
    id = this.id,
    text = this.text,
    type = this.type,
    timestamp = this.timestamp,
    isRead = this.isRead,
    isFavorite = this.isFavorite
)
