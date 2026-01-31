package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.misc

import com.google.gson.annotations.SerializedName

data class LifeSystemStateSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("loadLevel") val loadLevel: String,
    @SerializedName("executionMode") val executionMode: String,
    @SerializedName("stability") val stability: String,
    @SerializedName("entropy") val entropy: String,
    @SerializedName("updatedAt") val updatedAt: Long
)
