package com.romankozak.forwardappmobile.data.database.models

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
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
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
        Index(value = ["project_id"]),
        Index(value = ["note_document_id"]),
    ],
)
data class SystemAppEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "system_key") val systemKey: String,
    @ColumnInfo(name = "app_type") val appType: String = SystemAppType.NOTE_DOCUMENT.name,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "note_document_id") val noteDocumentId: String? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
)

enum class SystemAppType {
    NOTE_DOCUMENT,
}

object ReservedSystemAppKeys {
    const val MY_LIFE_CURRENT_STATE = "my-life-current-state"
}
