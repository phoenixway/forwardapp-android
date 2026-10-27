package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.recurrence.decodeTaskRecurrenceTemplate
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import javax.inject.Inject

data class OrientationKindPlan(
    val id: String, val title: String, val version: Long, val kind: OrientationKind,
    val assessmentRevisionId: String, val assessmentVersion: Long,
    val planningCount: Int, val recurrenceCount: Int,
)

/** Native semantic reclassification only. Independently owned downstream rows are not rewritten. */
class CanonicalOrientationKindCoordinator @Inject constructor(
    private val db: AppDatabase,
    private val orientations: CanonicalOrientationRepository,
) {
    suspend fun canOffer(id: String): Boolean = db.withTransaction {
        val subject = db.orientationDao().getManagedSubject(id)
        subject?.subjectType == "ORIENTATION" && !subject.isDeleted &&
            db.orientationDao().getOrientation(id)?.kind in SUBJECT_CONVERSION_TARGET_KINDS.map { it.name } &&
            db.orientationDao().getAllLegacyMappings().none { it.subjectId == id }
    }

    suspend fun preview(id: String): OrientationKindPlan = db.withTransaction {
        require(canOffer(id)) { "Зміна типу доступна лише для активних native орієнтирів звичайних типів" }
        val dao = db.orientationDao()
        val semantics = requireNotNull(orientations.getOrientationSemantics(id))
        val node = requireNotNull(dao.getOrientation(id))
        node.lifecycle?.let { OrientationLifecycle.valueOf(it) }
        ValueOrigin.valueOf(node.lifecycleOrigin)
        val current = dao.getAllAssessments().single { it.orientationId == id }
        require(current.version < Long.MAX_VALUE && semantics.subject.version < Long.MAX_VALUE)
        val planning = db.planningDao().getAllCommitments().filter { it.orientationId == id && !it.isDeleted }
        planning.forEach {
            it.toPlanningCommitmentStrict(com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry.INITIAL)
            require(db.planningDao().getScope(it.scopeId)?.isDeleted == false) { "Planning participation має недоступний scope" }
        }
        val refs = db.planningReferenceDao().getAll().filter { it.targetId == id && it.targetKind == "ORIENTATION" && !it.isDeleted }
        val authority = CanonicalPlanningReferenceRepository(db)
        refs.forEach { authority.requireOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.valueOf(it.ownerKind), it.ownerId), live = false) }
        val recurrenceCount = db.canonicalRecurringSeriesDao().getAllSync().count { series ->
            !series.isDeleted && series.kind == "TASK" && decodeTaskRecurrenceTemplate(series.templateJson, Gson()).planningSource?.let {
                it.kind.name == "ORIENTATION" && it.id == id
            } == true
        }
        CanonicalSubjectConversionStore(db).validateCurrentClosure()
        OrientationKindPlan(id, semantics.subject.title, semantics.subject.version, semantics.kind, current.revisionId, current.version,
            planning.size + refs.size, recurrenceCount)
    }

    suspend fun transform(plan: OrientationKindPlan, target: OrientationKind, now: Long = System.currentTimeMillis()): String = db.withTransaction {
        require(target in SUBJECT_CONVERSION_TARGET_KINDS && target != plan.kind) { "Оберіть інший підтриманий тип" }
        val dao = db.orientationDao()
        val eventId = "${plan.id}:ORIENTATION_KIND:${plan.version}"
        val history = db.managedSubjectConversionDao().getAll().filter { it.subjectId == plan.id }
        history.singleOrNull { it.id == eventId }?.let {
            require(it.targetOrientationKind == target.name && it.fromOrientationKind == plan.kind.name) { "Інша зміна типу вже виконана" }
            CanonicalSubjectConversionStore(db).validateCurrentClosure()
            return@withTransaction plan.id
        }
        require(preview(plan.id) == plan) { "Сутність або залежності змінилися. Відкрийте підтвердження знову." }
        val subject = requireNotNull(dao.getManagedSubject(plan.id))
        val node = requireNotNull(dao.getOrientation(plan.id))
        val current = dao.getAllAssessments().single { it.orientationId == plan.id }
        val oldRevision = dao.getAllAssessmentRevisions().single { it.id == current.revisionId }
        val assessment = assessmentAfterKindChange(plan.kind, target, decodeOrientationAssessmentJson(Gson(), oldRevision.assessmentJson))
        val at = maxOf(now, subject.updatedAt, current.updatedAt)
        val historicalKinds = historicalAssessmentKinds(history.map { it.toConversionEvidence() }, plan.kind, plan.version)
        val previousIds = dao.getAllAssessmentRevisions().filter { it.orientationId == plan.id && (it.id !in historicalKinds || it.id == current.revisionId) }.map { it.id }.sorted()
        orientations.saveOrientation(
            ManagedSubject(subject.id, subject.createdAt, at, null, false, subject.version + 1, ManagedSubjectType.ORIENTATION, subject.title, subject.description),
            OrientationNode(subject.id, target, node.lifecycle?.let(OrientationLifecycle::valueOf), ValueOrigin.valueOf(node.lifecycleOrigin), assessment),
            OrientationAssessmentRevision("${plan.id}:kind:${plan.version}:assessment", at, at, null, false, 1, plan.id, at, at,
                AssessmentRevisionSource.USER, "Explicit kind change ${plan.kind.name} -> ${target.name}", assessment),
        )
        // Current assessment has an independent version/creation identity, not the subject's version.
        val updated = dao.getAllAssessments().single { it.orientationId == plan.id }
        dao.upsertAssessments(listOf(updated.copy(createdAt = current.createdAt, version = current.version + 1)))
        val fact = ManagedSubjectConversionEntity(eventId, plan.id, 2, "ORIENTATION", "ORIENTATION", target.name,
            plan.version, plan.version + 1, null, 0, false, at, at, 1, null, plan.kind.name, Gson().toJson(previousIds))
        CanonicalSubjectConversionStore(db).merge(history + fact)
        CanonicalSubjectConversionStore(db).validateCurrentClosure()
        plan.id
    }
}
