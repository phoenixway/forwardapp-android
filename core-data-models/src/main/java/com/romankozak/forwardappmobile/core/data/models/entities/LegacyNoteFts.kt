package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.Fts4
import com.google.gson.annotations.SerializedName

@Fts4(contentEntity = LegacyNoteEntity::class)
@Entity(tableName = "notes_fts")
data class LegacyNoteFts(
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
)
