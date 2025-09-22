package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "custom_list_items",
    foreignKeys = [
        ForeignKey(
            entity = CustomListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"]), Index(value = ["parentId"])]
)
data class CustomListItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val parentId: String? = null,
    var content: String,
    var isCompleted: Boolean = false,
    var itemOrder: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
