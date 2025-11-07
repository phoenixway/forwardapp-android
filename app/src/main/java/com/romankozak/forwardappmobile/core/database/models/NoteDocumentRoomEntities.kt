package com.romankozak.forwardappmobile.core.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "note_documents")
data class NoteDocumentRoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    var name: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val content: String? = null,
    @ColumnInfo(defaultValue = "0") val lastCursorPosition: Int = 0,
)

@Entity(
    tableName = "note_document_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteDocumentRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NoteDocumentItemRoomEntity::class,
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
data class NoteDocumentItemRoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val parentId: String? = null,
    var content: String,
    var isCompleted: Boolean = false,
    var itemOrder: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)
