package com.romankozak.forwardappmobile.core.data.models.sync

fun SnapshotBundle.hasCanonicalOrientationPayload(): Boolean =
    legacyCanonicalOrientationFields().all { it != null }

fun requireValidCanonicalOrientationPayload(bundle: SnapshotBundle) {
    require(bundle.managedSubjectConversions == null || bundle.hasCanonicalOrientationPayload()) {
        "Semantic conversion history requires a complete canonical subject envelope"
    }
    val legacyFields = bundle.legacyCanonicalOrientationFields()
    val empty = legacyFields.all { it == null } && bundle.workspaces == null
    val legacyOrCurrentComplete = legacyFields.all { it != null }
    require(empty || legacyOrCurrentComplete) {
        "Canonical Orientation payload must contain none, the legacy 11 fields, or all 12 fields."
    }
}

private fun SnapshotBundle.legacyCanonicalOrientationFields(): List<Any?> =
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
    )
