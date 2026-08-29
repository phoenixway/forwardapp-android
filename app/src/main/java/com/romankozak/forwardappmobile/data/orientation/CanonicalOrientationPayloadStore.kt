package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.hasCanonicalOrientationPayload
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalOrientationPayload
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

internal suspend fun OrientationDao.storeCanonicalPayload(
    bundle: SnapshotBundle,
    merge: Boolean,
    workspaceDao: WorkspaceDao,
) {
    requireValidCanonicalOrientationPayload(bundle)
    if (!bundle.hasCanonicalOrientationPayload()) return
    validateCanonicalPayloadReferences(bundle)

    val incomingSubjects = requireNotNull(bundle.managedSubjects)
    val subjectsToWrite =
        if (merge) {
            mergeByFreshness(
                local = getAllManagedSubjects(),
                incoming = incomingSubjects,
                id = { it.id },
                version = { it.version },
                updatedAt = { it.updatedAt },
            )
        } else {
            incomingSubjects
        }
    val acceptedSubjectIds = subjectsToWrite.mapTo(hashSetOf()) { it.id }
    if (subjectsToWrite.isNotEmpty()) upsertManagedSubjects(subjectsToWrite)

    val orientations = requireNotNull(bundle.orientations).filter { !merge || it.subjectId in acceptedSubjectIds }
    val aspects = requireNotNull(bundle.aspects).filter { !merge || it.subjectId in acceptedSubjectIds }
    if (orientations.isNotEmpty()) upsertOrientations(orientations)
    if (aspects.isNotEmpty()) upsertAspects(aspects)

    bundle.workspaces?.let { rawIncoming ->
        val incoming = rawIncoming.map { it.normalizeProvenanceForPersistence() }
        val workspaces =
            mergeByFreshnessIfNeeded(
                merge,
                workspaceDao.getAll(),
                incoming,
                { it.id },
                { it.version },
                { it.updatedAt },
            )
        if (workspaces.isNotEmpty()) workspaceDao.upsert(workspaces)
    }

    val assessments =
        mergeByFreshnessIfNeeded(
            merge,
            getAllAssessments(),
            requireNotNull(bundle.orientationAssessments),
            { it.orientationId },
            { it.version },
            { it.updatedAt },
        )
    val revisions =
        mergeByFreshnessIfNeeded(
            merge,
            getAllAssessmentRevisions(),
            requireNotNull(bundle.orientationAssessmentRevisions),
            { it.id },
            { it.version },
            { it.updatedAt },
        )
    val mappings =
        mergeByFreshnessIfNeeded(
            merge,
            getAllLegacyMappings(),
            requireNotNull(bundle.legacySubjectMappings),
            { it.id },
            { it.version },
            { it.updatedAt },
        )
    val relations =
        mergeByFreshnessIfNeeded(
            merge,
            getAllOrientationRelations(),
            requireNotNull(bundle.orientationRelations),
            { it.id },
            { it.version },
            { it.updatedAt },
        )
    val aspectRefs =
        mergeByFreshnessIfNeeded(
            merge,
            getAllAspectOrientationRefs(),
            requireNotNull(bundle.aspectOrientationRefs),
            { it.id },
            { it.version },
            { it.updatedAt },
        )
    val bindings =
        mergeByFreshnessIfNeeded(
            merge,
            getAllWorkspaceBindings(),
            requireNotNull(bundle.workspaceBindings),
            { it.id },
            { it.version },
            { it.updatedAt },
        )
    val capabilities =
        mergeByFreshnessIfNeeded(
            merge,
            getAllWorkspaceCapabilities(),
            requireNotNull(bundle.workspaceCapabilityInstances),
            { it.id },
            { it.version },
            { it.updatedAt },
        )
    val views =
        mergeByFreshnessIfNeeded(
            merge,
            getAllSavedViews(),
            requireNotNull(bundle.savedOrientationViews),
            { it.id },
            { it.version },
            { it.updatedAt },
        )

    if (revisions.isNotEmpty()) upsertAssessmentRevisions(revisions)
    if (assessments.isNotEmpty()) upsertAssessments(assessments)
    if (mappings.isNotEmpty()) upsertLegacyMappings(mappings)
    if (relations.isNotEmpty()) upsertOrientationRelations(relations)
    if (aspectRefs.isNotEmpty()) upsertAspectOrientationRefs(aspectRefs)
    if (bindings.isNotEmpty()) upsertWorkspaceBindings(bindings)
    if (capabilities.isNotEmpty()) upsertWorkspaceCapabilities(capabilities)
    if (views.isNotEmpty()) upsertSavedViews(views)
}

private fun WorkspaceEntity.normalizeProvenanceForPersistence(): WorkspaceEntity {
    val rawProvenance: String? = provenance
    return when (rawProvenance) {
        null ->
            copy(
                provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                sourceContextId = id,
            )

        WorkspaceProvenance.CONTEXT_BACKED.name -> {
            require(sourceContextId == null || sourceContextId == id) {
                "Context-backed Workspace $id must use itself as sourceContextId"
            }
            if (sourceContextId == null) copy(sourceContextId = id) else this
        }

        WorkspaceProvenance.CANONICAL_ONLY.name -> {
            require(sourceContextId == null) {
                "Canonical-only Workspace $id must not have sourceContextId"
            }
            this
        }

        else -> throw IllegalArgumentException("Unknown Workspace provenance '$rawProvenance' for $id")
    }
}

private fun <T> mergeByFreshnessIfNeeded(
    merge: Boolean,
    local: List<T>,
    incoming: List<T>,
    id: (T) -> String,
    version: (T) -> Long,
    updatedAt: (T) -> Long,
): List<T> = if (merge) mergeByFreshness(local, incoming, id, version, updatedAt) else incoming

/** Returns only incoming winners, so local rows are never rewritten or re-acknowledged. */
internal fun <T> mergeByFreshness(
    local: List<T>,
    incoming: List<T>,
    id: (T) -> String,
    version: (T) -> Long,
    updatedAt: (T) -> Long,
): List<T> {
    val localById = local.associateBy(id)
    return incoming.filter { candidate ->
        val current = localById[id(candidate)] ?: return@filter true
        version(candidate) > version(current) ||
            (version(candidate) == version(current) && updatedAt(candidate) > updatedAt(current))
    }
}
