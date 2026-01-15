package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "structure_preset_items",
    foreignKeys = [
        ForeignKey(
            entity = StructurePreset::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["presetId"]),
        Index(value = ["presetId", "roleCode"], unique = true),
    ]
)
data class StructurePresetItem(
    @PrimaryKey val id: String,
    val presetId: String,
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    @ColumnInfo(defaultValue = "0") val mandatory: Boolean,
)
