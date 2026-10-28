package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.COMPOSITION_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.sync.datasource.CanonicalPlanningSyncPayload

/** Relations carry both endpoint aggregates/scopes and semantic dependencies; ACK stays dirty-only. */
internal fun planningCompositionTransportClosure(full: SnapshotBundle, dirty: CanonicalPlanningSyncPayload): SnapshotBundle {
    require(full.planningFormatVersion == COMPOSITION_PLANNING_FORMAT_VERSION && full.planningCommitmentRelations != null) {
        "Composition delta requires a v5 dependency frame"
    }
    val rows = requireNotNull(full.planningCommitmentRelations).associateBy { it.id }
    require(dirty.relations.all { rows[it.id]?.copy(syncedAt = null) == it.copy(syncedAt = null) }) {
        "Composition changed during delta assembly"
    }
    val commitments = requireNotNull(full.planningCommitments).associateBy { it.id }
    val scopes = requireNotNull(full.planningScopes).associateBy { it.id }
    val subjects = requireNotNull(full.managedSubjects).associateBy { it.id }
    val orientations = requireNotNull(full.orientations).mapTo(hashSetOf()) { it.subjectId }
    dirty.relations.forEach { edge ->
        require(edge.relationType == "COMPONENT_OF") { "Unsupported Planning composition type" }
        listOf(edge.sourceCommitmentId, edge.targetCommitmentId).forEach { id ->
            val endpoint = requireNotNull(commitments[id]) { "Missing composition endpoint in dependency frame: $id" }
            val scope = requireNotNull(scopes[endpoint.scopeId]) { "Missing composition scope" }
            val subject = requireNotNull(subjects[endpoint.orientationId]) { "Missing composition semantic dependency" }
            require(endpoint.orientationId in orientations) { "Missing Orientation aggregate in composition closure" }
            if (!edge.isDeleted) require(!endpoint.isDeleted && !scope.isDeleted && !subject.isDeleted && subject.subjectType == "ORIENTATION") {
                "Live composition requires live canonical endpoints"
            }
        }
    }
    return planningReferenceTransportClosure(full, dirty.references).copy(planningCommitmentRelations = dirty.relations)
}
