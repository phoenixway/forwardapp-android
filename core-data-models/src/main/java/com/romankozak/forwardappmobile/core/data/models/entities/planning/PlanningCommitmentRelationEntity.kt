package com.romankozak.forwardappmobile.core.data.models.entities.planning

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "planning_commitment_relations",
    foreignKeys = [
        ForeignKey(entity = PlanningCommitmentEntity::class, parentColumns = ["id"], childColumns = ["sourceCommitmentId"]),
        ForeignKey(entity = PlanningCommitmentEntity::class, parentColumns = ["id"], childColumns = ["targetCommitmentId"]),
    ],
    indices = [Index("sourceCommitmentId"), Index(value = ["targetCommitmentId", "relationType", "isDeleted", "relationOrder", "id"]),
        Index("updatedAt"), Index("syncedAt")])
data class PlanningCommitmentRelationEntity(
    @PrimaryKey val id: String,
    val sourceCommitmentId: String,
    val targetCommitmentId: String,
    val relationType: String,
    val relationOrder: Long,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
