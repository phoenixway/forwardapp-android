package com.romankozak.forwardappmobile.core.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "system_apps",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NoteDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_document_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["system_key"], unique = true),
        Index(value = ["context_id"]),
        Index(value = ["note_document_id"]),
    ],
)
// File: SystemAppEntity.kt
data class SystemAppEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "system_key") val systemKey: String,
    @ColumnInfo(name = "app_type") val appType: String = SystemAppType.NOTE_DOCUMENT.name,
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "note_document_id") val noteDocumentId: String? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    // Додаємо ці поля, щоб вони відповідали Snapshot:
    val version: Long = 0,
    val isDeleted: Boolean = false
)
enum class SystemAppType {
    NOTE_DOCUMENT,
}

object ReservedSystemAppKeys {
    const val MY_LIFE_CURRENT_STATE = "my-life-current-state"
}
