package com.romankozak.forwardappmobile.data.entitybrowser

import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/** Fixed bulk snapshots. No ProductRead, Context, graph or per-entity metadata lookup. */
@Singleton
class CanonicalEntityQueryRepository @Inject constructor(
    private val catalog: CanonicalEntityCatalogRepository,
    private val tags: CanonicalWorkspaceTagRepository,
    private val orientations: OrientationDao,
    private val semanticTags: com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticTagRepository,
) {
    fun observeCatalog() = combine(catalog.observeCatalog(), tags.observeLiveTagsByWorkspace(),
        orientations.observeAssessments(), orientations.observeScoringOverrides(), semanticTags.observeAllTags()) { entities, tags, assessments, overrides, subjectTags ->
        val overrideById = overrides.filterNot { it.isDeleted }.associateBy { it.orientationId }
        val scores = assessments.filterNot { it.isDeleted }.associate { assessment ->
            assessment.orientationId to canonicalQueryScores(assessment, overrideById[assessment.orientationId])
        }
        entities.map { item -> when (val ref = item.ref) {
            is CanonicalEntityRef.Workspace -> item.copy(tags = tags[ref.id].orEmpty().toSet())
            is CanonicalEntityRef.Orientation -> item.copy(scores = scores[ref.id].orEmpty(), tags = subjectTags[
                com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef(
                    com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType.ORIENTATION, ref.id)].orEmpty())
            is CanonicalEntityRef.Aspect -> item.copy(tags = subjectTags[
                com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef(
                    com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType.ASPECT, ref.id)].orEmpty())
        } }
    }.flowOn(Dispatchers.Default)
}
