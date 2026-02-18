package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    tableName = "note_documents",
    indices = [Index(value = ["contextId"], name = "index_note_documents_contextId")],
)
data class NoteDocumentEntity(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String = "",
    @SerializedName("name") var name: String,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") var updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("content") val content: String? = null,
    @ColumnInfo(defaultValue = "0") @SerializedName("lastCursorPosition") val lastCursorPosition: Int = 0,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
)
