package com.romankozak.forwardappmobile.core.data.models.entities.planning

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/** Explicit product reference, never Orientation participation or structural ownership. */
enum class PlanningReferenceOwnerKind { SCOPE, TACTICAL_MISSION, ARC_QUEST, DAY_TASK, DAY_FOCUS }
enum class PlanningReferenceTargetKind { ORIENTATION, ASPECT, WORKSPACE }
enum class PlanningReferenceRole { SOURCE, RELATED }

data class PlanningReferenceOwner(val kind: PlanningReferenceOwnerKind, val id: String) {
    init {
        require(id.isNotBlank())
        require(kind != PlanningReferenceOwnerKind.TACTICAL_MISSION || id.toLongOrNull()?.takeIf { it > 0 }?.toString() == id)
    }
}

data class PlanningReferenceTarget(val kind: PlanningReferenceTargetKind, val id: String) {
    init { require(id.isNotBlank()) }
}

@Entity(
    tableName = "planning_references",
    indices = [
        Index(value = ["ownerKind", "ownerId", "targetKind", "targetId", "role"], unique = true),
        Index(value = ["targetKind", "targetId"]),
        Index("updatedAt"), Index("syncedAt"),
    ],
)
data class PlanningReferenceEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("ownerKind") val ownerKind: String,
    @SerializedName("ownerId") val ownerId: String,
    @SerializedName("targetKind") val targetKind: String,
    @SerializedName("targetId") val targetId: String,
    @SerializedName("role") val role: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)
