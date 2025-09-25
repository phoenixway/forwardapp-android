package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_folders")
data class ConversationFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
)