package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.features.contexts.data.models.NoteDocumentEntity
import java.util.UUID

@Entity(
    tableName = "note_document_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NoteDocumentItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["listId"], name = "index_note_document_items_listId"),
        Index(value = ["parentId"], name = "index_note_document_items_parentId"),
    ],
)
data class NoteDocumentItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val parentId: String? = null,
    var content: String,
    var isCompleted: Boolean = false,
    var itemOrder: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)
