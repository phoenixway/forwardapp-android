package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = LegacyNoteEntity::class)
@Entity(tableName = "notes_fts")
data class LegacyNoteFts(
    val title: String,
    val content: String,
)
