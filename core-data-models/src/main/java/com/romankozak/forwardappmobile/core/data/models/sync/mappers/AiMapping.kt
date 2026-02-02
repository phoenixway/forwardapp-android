// File: AiMapping.kt

package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.ai.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.*

// --- Conversation Mapping ---
fun ConversationEntity.toSnapshot(): ConversationSnapshot = ConversationSnapshot(
    id = this.id.toString(), // Long -> String
    title = this.title,
    folderId = this.folderId?.toString(), // Long? -> String?
    createdAt = this.creationTimestamp,
    updatedAt = this.creationTimestamp, // Використовуємо час створення, якщо немає окремого updatedAt
    version = this.version,
    isDeleted = this.isDeleted,
)

fun ConversationSnapshot.toEntity(): ConversationEntity = ConversationEntity(
    id = this.id.toLongOrNull() ?: 0L, // String -> Long
    title = this.title,
    creationTimestamp = this.createdAt,
    folderId = this.folderId?.toLongOrNull(), // String? -> Long?
    version = this.version,
    isDeleted = this.isDeleted,
)

// --- Chat Message Mapping ---
fun ChatMessageEntity.toSnapshot(): ChatMessageSnapshot = ChatMessageSnapshot(
    id = this.id.toString(),
    conversationId = this.conversationId.toString(),
    text = this.text,
    isFromUser = this.isFromUser,
    isError = this.isError,
    timestamp = this.timestamp,
    version = this.version,
    isDeleted = this.isDeleted,
)

fun ChatMessageSnapshot.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = this.id.toLongOrNull() ?: 0L,
    conversationId = this.conversationId.toLongOrNull() ?: 0L,
    text = this.text,
    isFromUser = this.isFromUser,
    isError = this.isError,
    timestamp = this.timestamp,
    version = this.version,
    isDeleted = this.isDeleted,
)

// --- AI Insight Mapping ---
fun AiInsightEntity.toSnapshot(): AiInsightSnapshot = AiInsightSnapshot(
    id = this.id, // Тут і там String, конвертація не потрібна
    text = this.text,
    type = this.type,
    timestamp = this.timestamp,
    isRead = this.isRead,
    isFavorite = this.isFavorite,
    version = this.version,
    isDeleted = this.isDeleted,
)

fun AiInsightSnapshot.toEntity(): AiInsightEntity = AiInsightEntity(
    id = this.id,
    text = this.text,
    type = this.type,
    timestamp = this.timestamp,
    isRead = this.isRead,
    isFavorite = this.isFavorite,
    version = this.version,
    isDeleted = this.isDeleted,
)