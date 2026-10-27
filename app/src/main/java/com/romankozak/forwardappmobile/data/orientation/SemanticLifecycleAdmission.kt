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
    val orientations = subjects.filter { !it.isDeleted && it.subjectType == "ORIENTATION" }.mapTo(hashSetOf()) { it.id }
    val aspectIds = subjects.filter { !it.isDeleted && it.subjectType == "ASPECT" }.mapTo(hashSetOf()) { it.id }
    require(relations.none { !it.isDeleted && (it.fromOrientationId !in orientations || it.toOrientationId !in orientations) }) {
        "Live Orientation relation references a missing/deleted semantic endpoint"
    }
    require(refs.none { !it.isDeleted && (it.aspectId !in aspectIds || it.orientationId !in orientations) }) {
        "Live Aspect association references a missing/deleted semantic endpoint"
    }
    require(bindings.none { !it.isDeleted && it.subjectId !in live }) {
        "Live WorkspaceBinding references a missing/deleted semantic endpoint"
    }
    require(aspects.none { it.subjectId in live && it.parentAspectId != null && it.parentAspectId !in aspectIds }) {
        "Live Aspect has a missing/deleted taxonomy parent"
    }
}
