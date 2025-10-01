package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "custom_lists",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId"])],
)
data class CustomListEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    var name: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val content: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val lastCursorPosition: Int = 0,
)
