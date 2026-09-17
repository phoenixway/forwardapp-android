package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * contextId is a stable logical association id, not Context-row ownership.
 * It may refer to an ordinary Context or an exact canonical System Workspace.
 */
@Entity(
    tableName = "scripts",
    indices = [
        Index(value = ["contextId"], name = "index_scripts_contextId"),
        Index(value = ["name"], name = "index_scripts_name"),
    ],
)
data class ScriptEntity(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
)
