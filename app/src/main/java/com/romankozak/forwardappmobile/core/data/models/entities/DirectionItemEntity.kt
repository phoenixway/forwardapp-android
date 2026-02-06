package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "direction_items",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contextId")]
)
data class DirectionItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val contextId: String,
    val text: String,
    val itemOrder: Int
)
