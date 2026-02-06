package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "system_key") @SerializedName("systemKey") val systemKey: String,
    @ColumnInfo(name = "app_type") @SerializedName("appType") val appType: String = SystemAppType.NOTE_DOCUMENT.name,
    @SerializedName(value = "contextId", alternate = ["projectId"])
    @ColumnInfo(name = "context_id") val contextId: String = "",
    @ColumnInfo(name = "note_document_id") @SerializedName("noteDocumentId") val noteDocumentId: String? = null,
    @ColumnInfo(name = "createdAt") @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    // Додаємо ці поля, щоб вони відповідали Snapshot:
    @SerializedName("version") val version: Long = 0,
    @SerializedName("isDeleted") val isDeleted: Boolean = false
)
enum class SystemAppType {
    NOTE_DOCUMENT,
}

object ReservedSystemAppKeys {
    const val MY_LIFE_CURRENT_STATE = "my-life-current-state"
}
