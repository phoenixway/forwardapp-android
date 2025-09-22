package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFts(
    val title: String,
    val content: String,
)
