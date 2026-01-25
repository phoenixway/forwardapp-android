package com.romankozak.forwardappmobile.features.attachments.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import java.util.UUID

@Entity(
    tableName = "note_documents",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.Companion.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId"], name = "index_note_documents_projectId")],
)
data class NoteDocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    var name: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val content: String? = null,
    @ColumnInfo(defaultValue = "0") val lastCursorPosition: Int = 0,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)
