package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orientation_assessments",
    foreignKeys = [
        ForeignKey(
            entity = OrientationEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["orientationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("revisionId", unique = true),
        Index("importanceValue"),
        Index("impactValue"),
        Index("attentionTierValue"),
    ],
)
data class OrientationAssessmentEntity(
    @PrimaryKey val orientationId: String,
    val revisionId: String,
    val importanceValue: String?,
    val importanceOrigin: String,
    val impactValue: String?,
    val impactOrigin: String,
    val breadthValue: String?,
    val breadthOrigin: String,
    val expectedSpanValue: String?,
    val expectedSpanOrigin: String,
    val targetWindowValue: String?,
    val targetWindowOrigin: String,
    val attentionTierValue: String?,
    val attentionTierOrigin: String,
    val commitmentValue: String?,
    val commitmentOrigin: String,
    val confidenceValue: String?,
    val confidenceOrigin: String,
    val provenanceJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "orientation_assessment_revisions",
    foreignKeys = [
        ForeignKey(
            entity = OrientationEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["orientationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["orientationId", "effectiveFrom"]), Index("recordedAt")],
)
data class OrientationAssessmentRevisionEntity(
    @PrimaryKey val id: String,
    val orientationId: String,
    val effectiveFrom: Long,
    val recordedAt: Long,
    val source: String,
    val reason: String?,
    val assessmentJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
