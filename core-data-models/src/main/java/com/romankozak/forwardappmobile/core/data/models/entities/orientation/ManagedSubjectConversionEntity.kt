package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Immutable semantic lifecycle evidence, never another current subtype or H1 node. */
@Entity(tableName = "managed_subject_conversions", indices = [
    Index("subjectId"), Index(value = ["subjectId", "toSubjectVersion"], unique = true),
])
data class ManagedSubjectConversionEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val formatVersion: Int,
    val fromType: String,
    val toType: String,
    val targetOrientationKind: String,
    val fromSubjectVersion: Long,
    val toSubjectVersion: Long,
    val previousAspectParentId: String?,
    val previousAspectOrder: Long,
    val previousAspectArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val syncedAt: Long?,
    val fromOrientationKind: String? = null,
    val previousAssessmentRevisionIdsJson: String? = null,
)

fun managedSubjectConversionId(subjectId: String, fromVersion: Long): String =
    "$subjectId:ASPECT:ORIENTATION:$fromVersion"

/** Shape only; the repository must also verify exact canonical subject/subtype closure. */
fun ManagedSubjectConversionEntity.requireValidConversionShape() {
    toConversionEvidence().requireValidShape()
}

fun ManagedSubjectConversionEntity.toConversionEvidence() = com.romankozak.forwardappmobile.shared.core.models.orientation.SubjectConversionEvidence(
    id, subjectId, formatVersion, fromType, toType, targetOrientationKind, fromSubjectVersion, toSubjectVersion,
    previousAspectParentId, previousAspectOrder, previousAspectArchived, createdAt, updatedAt, version, syncedAt,
    fromOrientationKind, previousAssessmentRevisionIdsJson?.let { raw ->
        com.google.gson.JsonParser.parseString(raw).asJsonArray.map { element ->
            require(element.isJsonPrimitive && element.asJsonPrimitive.isString) { "Historical revision ID must be a string" }
            element.asString
        }
    })
