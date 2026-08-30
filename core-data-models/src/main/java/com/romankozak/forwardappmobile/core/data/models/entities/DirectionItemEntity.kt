package com.romankozak.forwardappmobile.core.data.models.entities

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class DirectionItemEntity(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String,
    @SerializedName("text") val text: String,
    @SerializedName("linkedContextId") val linkedContextId: String? = null,
    @SerializedName("itemOrder") val itemOrder: Int,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0
)
