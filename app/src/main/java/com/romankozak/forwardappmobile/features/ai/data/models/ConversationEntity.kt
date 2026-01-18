package com.romankozak.forwardappmobile.features.ai.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["folderId"])] // <-- ADD THIS LINE
)data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var title: String,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val folderId: Long? = null
)
