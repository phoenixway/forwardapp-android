package com.romankozak.forwardappmobile.core.database.models

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = LegacyNoteRoomEntity::class)
@Entity(tableName = "notes_fts")
data class LegacyNoteFts(
    val title: String,
    val content: String,
)
