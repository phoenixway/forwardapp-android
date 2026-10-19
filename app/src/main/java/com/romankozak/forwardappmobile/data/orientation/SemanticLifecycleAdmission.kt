package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity

/** Tombstoned edges retain endpoint evidence; live edges must never revive deleted sources. */
internal fun requireLiveSemanticEndpoints(
    subjects: List<ManagedSubjectEntity>,
    relations: List<OrientationRelationEntity>,
    refs: List<AspectOrientationRefEntity>,
    bindings: List<WorkspaceBindingEntity>,
    aspects: List<AspectEntity>,
) {
    val live = subjects.filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.id }
    require(relations.none { !it.isDeleted && (it.fromOrientationId !in live || it.toOrientationId !in live) }) {
        "Live Orientation relation references a missing/deleted semantic endpoint"
    }
    require(refs.none { !it.isDeleted && (it.aspectId !in live || it.orientationId !in live) }) {
        "Live Aspect association references a missing/deleted semantic endpoint"
    }
    require(bindings.none { !it.isDeleted && it.subjectId !in live }) {
        "Live WorkspaceBinding references a missing/deleted semantic endpoint"
    }
    require(aspects.none { it.subjectId in live && it.parentAspectId != null && it.parentAspectId !in live }) {
        "Live Aspect has a missing/deleted taxonomy parent"
    }
}
