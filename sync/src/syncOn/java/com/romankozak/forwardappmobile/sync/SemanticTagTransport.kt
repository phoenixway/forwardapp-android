package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectTagRefEntity
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload

/** Timestamp export must notice tag-only edits even after the dirty version was acknowledged. */
internal fun SnapshotBundle.semanticTagChangesSince(timestamp: Long): CanonicalOrientationSyncPayload {
    if (managedSubjectTagRefs.orEmpty().none { it.updatedAt > timestamp } &&
        managedSubjectConversions.orEmpty().none { it.updatedAt > timestamp }) return CanonicalOrientationSyncPayload()
    return CanonicalOrientationSyncPayload(
        managedSubjects = requireNotNull(managedSubjects), orientations = requireNotNull(orientations), aspects = requireNotNull(aspects),
        assessments = requireNotNull(orientationAssessments), assessmentRevisions = requireNotNull(orientationAssessmentRevisions),
        scoringOverrides = orientationScoringOverrides.orEmpty(), legacyMappings = requireNotNull(legacySubjectMappings),
        relations = requireNotNull(orientationRelations), aspectRefs = requireNotNull(aspectOrientationRefs),
        workspaces = workspaces.orEmpty(), workspaceBindings = requireNotNull(workspaceBindings),
        workspaceCapabilities = requireNotNull(workspaceCapabilityInstances), savedViews = requireNotNull(savedOrientationViews),
        semanticTagRefs = managedSubjectTagRefs.orEmpty(),
        subjectConversions = managedSubjectConversions.orEmpty(),
    )
}

/** Only metadata for already-proven selected semantic closure, never a source-wide implicit import. */
internal fun SnapshotBundle.withSemanticTagsFrom(source: SnapshotBundle): SnapshotBundle {
    val owners = managedSubjects.orEmpty().mapTo(hashSetOf()) { it.subjectType to it.id }
    return copy(managedSubjectTagRefs = source.managedSubjectTagRefs?.filter { (it.subjectType to it.subjectId) in owners },
        managedSubjectConversions = source.managedSubjectConversions?.filter { (it.toType to it.subjectId) in owners })
}

/** Dirty/full reads are separate frames. Never send a ref outside the chosen owner closure or ACK an unsent version. */
internal fun semanticTagsForDelta(
    full: List<ManagedSubjectTagRefEntity>?,
    dirty: List<ManagedSubjectTagRefEntity>,
    selectedSubjects: List<ManagedSubjectEntity>?,
): List<ManagedSubjectTagRefEntity>? {
    val owners = selectedSubjects?.mapTo(hashSetOf()) { it.subjectType to it.id }
    if (owners == null) { require(dirty.isEmpty()); return null }
    require(dirty.all { (it.subjectType to it.subjectId) in owners }) { "Dirty semantic tag has no selected typed owner" }
    if (full == null && dirty.isEmpty()) return null
    val rows = linkedMapOf<String, ManagedSubjectTagRefEntity>()
    (full.orEmpty() + dirty).filter { (it.subjectType to it.subjectId) in owners }.forEach { incoming ->
        val old = rows[incoming.id]
        when {
            old == null || incoming.version > old.version -> rows[incoming.id] = incoming
            incoming.version < old.version -> Unit
            else -> require(old.copy(syncedAt = null) == incoming.copy(syncedAt = null)) { "Divergent semantic tag snapshot frames" }
        }
    }
    return rows.values.toList()
}
