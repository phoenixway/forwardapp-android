package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("presetId") val presetId: String,
    @SerializedName("entityType") val entityType: String,
    @SerializedName("roleCode") val roleCode: String,
    @SerializedName("containerType") val containerType: String?,
    @SerializedName("title") val title: String,
    @ColumnInfo(defaultValue = "0") @SerializedName("mandatory") val mandatory: Boolean,
    // Додаємо технічні поля для відповідності Snapshot:
    @SerializedName("itemOrder") val itemOrder: Long = 0, // Ви намагалися передати 'order'
    @SerializedName("version") val version: Long = 0,
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("isDeleted") val isDeleted: Boolean = false
)