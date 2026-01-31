package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

import com.google.gson.annotations.SerializedName

data class LinkItemEntitySnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("linkData") val linkData: RelatedLinkSnapshot,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)