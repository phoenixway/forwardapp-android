package com.romankozak.forwardappmobile.core.data.models.entities.ai

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["conversationId"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) @SerializedName("id") val id: Long = 0,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("text") val text: String,
    @SerializedName("isFromUser") val isFromUser: Boolean,
    @SerializedName("isError") val isError: Boolean = false,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("isStreaming") val isStreaming: Boolean = false,
    @SerializedName("version") val version: Long = 0L,
    @SerializedName("isDeleted") val isDeleted: Boolean = false
)
