package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.ForeignKey

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = ConversationFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var title: String,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val folderId: Long? = null
)
