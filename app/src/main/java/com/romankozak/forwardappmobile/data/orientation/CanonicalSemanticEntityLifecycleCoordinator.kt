package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Deliberately excludes kinds with specialized/compatibility lifecycle owners. */
val genericDeletableOrientationKinds: Set<OrientationKind> = setOf(
    OrientationKind.INTENT,
    OrientationKind.PROJECT,
    OrientationKind.MILESTONE,
    OrientationKind.ONGOING_STANDARD,
    OrientationKind.OPPORTUNITY,
)

/** A captured lifecycle intent; the delete command revalidates type, version and dependents. */
data class SemanticEntityDeletionTarget(
    val subjectId: String,
    val subjectType: ManagedSubjectType,
    val title: String,
    val version: Long,
)

/**
 * Semantic-source lifecycle, independent of the product surface invoking it.
 * Identity, semantic relation and typed H1 authorities share one Room transaction.
 * Never deletes a Workspace, Context, Planning participation or presentation container.
 */
@Singleton
class CanonicalSemanticEntityLifecycleCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val orientations: CanonicalOrientationRepository,
    private val aspects: CanonicalAspectRepository,
    private val graph: CanonicalOrientationGraphRepository,
    private val aspectLinks: CanonicalAspectLinksRepository,
    private val bindings: CanonicalWorkspaceBindingRepository,
    private val hierarchy: HierarchyPlacementLifecycleCoordinator,
) {
    /** Read-only capability, shared lifecycle policy rather than an editor-owned deletion rule. */
    suspend fun deletionTarget(subjectId: String): SemanticEntityDeletionTarget? = database.withTransaction {
        val dao = database.orientationDao()
        val subject = dao.getManagedSubject(subjectId)?.takeUnless { it.isDeleted } ?: return@withTransaction null
        if (dao.getAllLegacyMappings().any { it.subjectId == subjectId }) return@withTransaction null
        val type = ManagedSubjectType.entries.singleOrNull { it.name == subject.subjectType } ?: return@withTransaction null
        when (type) {
            ManagedSubjectType.ORIENTATION -> {
                val kind = dao.getOrientation(subjectId)?.kind
                if (genericDeletableOrientationKinds.none { it.name == kind }) return@withTransaction null
            }
            ManagedSubjectType.ASPECT -> if (dao.getAspect(subjectId) == null) return@withTransaction null
        }
        SemanticEntityDeletionTarget(subjectId, type, subject.title, subject.version)
    }

    suspend fun tombstone(
        subjectId: String,
        subjectType: ManagedSubjectType,
        expectedVersion: Long,
        now: Long = System.currentTimeMillis(),
    ) = database.withTransaction {
        currentCoroutineContext().ensureActive()
        val dao = database.orientationDao()
        val subject = requireNotNull(dao.getManagedSubject(subjectId)) { "Semantic subject does not exist" }
        require(subject.subjectType == subjectType.name) { "Semantic subject type mismatch" }
        require(subject.version == expectedVersion) { "Semantic subject version conflict" }
        require(dao.getAllLegacyMappings().none { it.subjectId == subjectId }) {
            "Legacy-mapped subject requires its specialized lifecycle owner"
        }
        if (subjectType == ManagedSubjectType.ORIENTATION) {
            val orientation = requireNotNull(dao.getAllOrientations().singleOrNull { it.subjectId == subjectId }) {
                "Canonical Orientation node does not exist"
            }
            require(OrientationKind.valueOf(orientation.kind) in genericDeletableOrientationKinds) {
                "Orientation kind ${orientation.kind} requires its specialized lifecycle owner"
            }
        } else {
            requireNotNull(dao.getAspect(subjectId)) { "Canonical Aspect node does not exist" }
        }
        if (subject.isDeleted) return@withTransaction
        require(database.planningDao().getAllCommitments().none { !it.isDeleted && it.orientationId == subjectId }) {
            "End Planning participation through its authority before deleting the semantic subject"
        }
        val edges = dao.getAllOrientationRelations().filter {
            !it.isDeleted && (it.fromOrientationId == subjectId || it.toOrientationId == subjectId)
        }
        require(edges.none { it.relationType == OrientationRelationType.PART_OF.name }) {
            "Historical PART_OF requires its explicit specialized lifecycle"
        }
        if (subjectType == ManagedSubjectType.ORIENTATION) orientations.requireCompleteActiveOrientationAggregate(subjectId)

        hierarchy.retireSemanticTargetAppearances(subjectId, now)
        CanonicalSemanticTagRepository(database).retireForSubject(
            com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef(subjectType, subjectId), now)
        edges.forEach { graph.removeRelation(it.id, it.version, now) }
        dao.getAllAspectOrientationRefs().filter {
            !it.isDeleted && (it.aspectId == subjectId || it.orientationId == subjectId)
        }.forEach { aspectLinks.unlinkOrientation(it.id, now) }
        dao.getAllWorkspaceBindings().filter { !it.isDeleted && it.subjectId == subjectId }
            .forEach { bindings.unbind(it.id, now) }

        if (subjectType == ManagedSubjectType.ASPECT) {
            // Existing Aspect lifecycle owns taxonomy-child promotion and subtype retention.
            aspects.tombstone(subjectId, now)
        } else {
            dao.getAllAssessments().single { it.orientationId == subjectId }.let {
                dao.upsertAssessments(listOf(it.copy(isDeleted = true, version = it.version + 1, updatedAt = now, syncedAt = null)))
            }
            dao.getScoringOverride(subjectId)?.takeUnless { it.isDeleted }?.let {
                dao.upsertScoringOverrides(listOf(it.copy(isDeleted = true, version = it.version + 1, updatedAt = now, syncedAt = null)))
            }
            // Assessment revisions are immutable historical evidence, not active authority.
            // The subtype row remains attached to the tombstoned ManagedSubject.
            dao.upsertManagedSubjects(listOf(subject.copy(isDeleted = true, version = subject.version + 1, updatedAt = now, syncedAt = null)))
        }
        currentCoroutineContext().ensureActive()
    }
}
