package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

import com.google.gson.annotations.SerializedName

data class RelatedLinkSnapshot(
    @SerializedName("type") val type: String?,
    @SerializedName("target") val target: String,
    @SerializedName("displayName") val displayName: String?
)
