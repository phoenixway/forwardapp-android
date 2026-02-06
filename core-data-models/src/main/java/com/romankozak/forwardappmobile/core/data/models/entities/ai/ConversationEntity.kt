package com.romankozak.forwardappmobile.core.data.models.entities.ai

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["folderId"])], // <-- ADD THIS LINE
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") var title: String,
    @SerializedName("creationTimestamp") val creationTimestamp: Long = System.currentTimeMillis(),
    @SerializedName("folderId") val folderId: Long? = null,
    @SerializedName("version") val version: Long = 0L,
    @SerializedName("isDeleted") val isDeleted: Boolean = false
)
