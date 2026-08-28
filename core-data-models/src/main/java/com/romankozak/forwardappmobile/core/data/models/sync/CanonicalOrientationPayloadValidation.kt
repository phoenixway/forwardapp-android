package com.romankozak.forwardappmobile.core.data.models.sync

private const val CANONICAL_ORIENTATION_FIELD_COUNT = 11

fun SnapshotBundle.hasCanonicalOrientationPayload(): Boolean =
    canonicalOrientationFieldCount() == CANONICAL_ORIENTATION_FIELD_COUNT

fun requireValidCanonicalOrientationPayload(bundle: SnapshotBundle) {
    val count = bundle.canonicalOrientationFieldCount()
    require(count == 0 || count == CANONICAL_ORIENTATION_FIELD_COUNT) {
        "Canonical Orientation payload must contain either none or all $CANONICAL_ORIENTATION_FIELD_COUNT fields."
    }
}

private fun SnapshotBundle.canonicalOrientationFieldCount(): Int =
    listOf(
        managedSubjects,
        orientations,
        aspects,
        orientationAssessments,
        orientationAssessmentRevisions,
        legacySubjectMappings,
        orientationRelations,
        aspectOrientationRefs,
        workspaceBindings,
        workspaceCapabilityInstances,
        savedOrientationViews,
    ).count { it != null }
