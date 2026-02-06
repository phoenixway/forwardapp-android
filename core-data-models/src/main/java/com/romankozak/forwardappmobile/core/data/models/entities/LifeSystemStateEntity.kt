package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "life_system_state")
data class LifeSystemStateEntity(
    @PrimaryKey @SerializedName("id") val id: String = "singleton",
    @SerializedName("loadLevel") val loadLevel: String,
    @SerializedName("executionMode") val executionMode: String,
    @SerializedName("stability") val stability: String,
    @SerializedName("entropy") val entropy: String,
    @SerializedName("updatedAt") val updatedAt: Long,
)