package com.romankozak.forwardappmobile.core.data.models.entities.ai

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "conversation_folders")
data class ConversationFolderEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String,
)
