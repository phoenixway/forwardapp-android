@file:OptIn(kotlin.js.ExperimentalJsExport::class)
package com.romankozak.forwardappmobile.shared.core.models.orientation

import kotlin.js.JsExport

/** The first generation is deliberately one-way and native-only. */
val SUBJECT_CONVERSION_TARGET_KINDS: Set<OrientationKind> = setOf(
    OrientationKind.PROJECT, OrientationKind.INTENT, OrientationKind.MILESTONE,
    OrientationKind.ONGOING_STANDARD, OrientationKind.OPPORTUNITY,
)

@JsExport
data class SubjectConversionEvidence(
    val id: String, val subjectId: String, val formatVersion: Int,
    val fromType: String, val toType: String, val targetOrientationKind: String,
    val fromSubjectVersion: Long, val toSubjectVersion: Long,
    val previousAspectParentId: String?, val previousAspectOrder: Long, val previousAspectArchived: Boolean,
    val createdAt: Long, val updatedAt: Long, val version: Long, val syncedAt: Long?,
    val fromOrientationKind: String? = null,
    val previousAssessmentRevisionIds: List<String>? = null,
) {
    fun requireValidShape() {
        require(subjectId.isNotBlank() && toType == "ORIENTATION") { "Invalid conversion owner/type" }
        when (formatVersion) {
            1 -> require(fromType == "ASPECT" && id == "$subjectId:ASPECT:ORIENTATION:$fromSubjectVersion" &&
                fromOrientationKind == null && previousAssessmentRevisionIds == null) { "Invalid Aspect conversion shape" }
            2 -> {
                require(fromType == "ORIENTATION" && id == "$subjectId:ORIENTATION_KIND:$fromSubjectVersion") { "Invalid kind change identity" }
                require(fromOrientationKind in SUBJECT_CONVERSION_TARGET_KINDS.map { it.name } && fromOrientationKind != targetOrientationKind) { "Unsupported source kind" }
                require(previousAspectParentId == null && previousAspectOrder == 0L && !previousAspectArchived) { "Kind change has no Aspect taxonomy" }
                val ids = requireNotNull(previousAssessmentRevisionIds) { "Missing historical assessment closure" }
                require(ids.isNotEmpty() && ids.all { it.isNotBlank() } && ids.distinct().size == ids.size) { "Invalid historical revision identities" }
            }
            else -> error("Unsupported semantic conversion format")
        }
        require(fromSubjectVersion >= 1 && fromSubjectVersion < Long.MAX_VALUE && toSubjectVersion == fromSubjectVersion + 1) { "Invalid conversion subject versions" }
        require(version == 1L && createdAt >= 0 && updatedAt == createdAt && (syncedAt == null || syncedAt >= 0)) { "Conversion facts are immutable" }
        require(previousAspectParentId != subjectId) { "Conversion has a self-parent" }
        require(targetOrientationKind in SUBJECT_CONVERSION_TARGET_KINDS.map { it.name }) { "Unsupported conversion target kind" }
    }
}
