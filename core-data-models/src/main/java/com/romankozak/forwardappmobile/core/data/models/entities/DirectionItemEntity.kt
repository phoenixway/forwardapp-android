package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String,
    @SerializedName("text") val text: String,
    @SerializedName("itemOrder") val itemOrder: Int,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") @SerializedName("version") val version: Long = 0
)
