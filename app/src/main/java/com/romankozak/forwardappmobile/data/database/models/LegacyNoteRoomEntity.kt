package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class LegacyNoteRoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    var title: String,
    var content: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)
