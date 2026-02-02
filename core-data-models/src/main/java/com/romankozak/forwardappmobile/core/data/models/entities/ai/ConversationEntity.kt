package com.romankozak.forwardappmobile.core.data.models.entities.ai

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["folderId"])], // <-- ADD THIS LINE
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var title: String,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val folderId: Long? = null,
    val version: Long = 0L,
    val isDeleted: Boolean = false
)
