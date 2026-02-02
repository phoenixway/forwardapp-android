package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "structure_preset_items",
    foreignKeys = [
        ForeignKey(
            entity = ContextRoleProfile::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["presetId"]),
        Index(value = ["presetId", "roleCode"], unique = true),
    ],
)
data class ContextRoleProfileItem(
    @PrimaryKey val id: String,
    val presetId: String,
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    @ColumnInfo(defaultValue = "0") val mandatory: Boolean,
    // Додаємо технічні поля для відповідності Snapshot:
    val itemOrder: Long = 0, // Ви намагалися передати 'order'
    val version: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)