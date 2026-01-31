package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

import com.google.gson.annotations.SerializedName

data class BacklogOrderSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("listId") val listId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("order") val order: Long,
    @SerializedName("orderVersion") val orderVersion: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)