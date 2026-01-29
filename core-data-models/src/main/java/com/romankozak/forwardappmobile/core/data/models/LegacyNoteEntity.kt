package com.romankozak.forwardappmobile.core.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["contextId"])],
)
data class LegacyNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val contextId: String,
    var title: String,
    var content: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)
