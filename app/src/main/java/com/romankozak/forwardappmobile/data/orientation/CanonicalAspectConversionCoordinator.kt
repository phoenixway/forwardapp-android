package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.recurrence.decodeTaskRecurrenceTemplate
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import javax.inject.Inject

data class AspectConversionPlan(
    val subjectId: String, val title: String, val version: Long, val parentId: String?,
    val childIds: List<String>, val memberships: Int, val tags: Int, val planningReferences: Int,
    val dependencyVersions: Map<String, Long>,
)

/** Coordinates existing authorities; never owns H1, Workspace or Planning participation. */
class CanonicalAspectConversionCoordinator @Inject constructor(
    private val db: AppDatabase,
    private val orientations: CanonicalOrientationRepository,
    private val aspects: CanonicalAspectRepository,
    private val links: CanonicalAspectLinksRepository,
    private val tags: CanonicalSemanticTagRepository,
    private val references: CanonicalPlanningReferenceRepository,
) {
    suspend fun canOffer(id: String): Boolean = db.withTransaction {
        val subject = db.orientationDao().getManagedSubject(id)
        subject?.let { !it.isDeleted && it.subjectType == "ASPECT" } == true &&
            db.orientationDao().getAspect(id)?.archived == false &&
            db.orientationDao().getAllLegacyMappings().none { it.subjectId == id } &&
            db.managedSubjectConversionDao().getAll().none { it.subjectId == id }
    }
    suspend fun preview(id: String): AspectConversionPlan = db.withTransaction {
        val dao = db.orientationDao()
        val subject = requireNotNull(dao.getManagedSubject(id)) { "Сутність недоступна" }
        require(!subject.isDeleted && subject.subjectType == "ASPECT") { "Перетворювати можна лише активний Aspect" }
        require(dao.getAllLegacyMappings().none { it.subjectId == id } && db.managedSubjectConversionDao().getAll().none { it.subjectId == id }) {
            "Історичні та вже перетворені сутності поки не підтримуються"
        }
        val node = requireNotNull(dao.getAspect(id))
        require(!node.archived && dao.getOrientation(id) == null) { "Спершу відновіть Aspect з архіву" }
        require(dao.getAllAssessments().none { it.orientationId == id } && dao.getAllAssessmentRevisions().none { it.orientationId == id } &&
            dao.getAllScoringOverrides().none { it.orientationId == id }) { "Aspect має несумісний Orientation assessment" }
        val subjects = dao.getAllManagedSubjects().associateBy { it.id }
        require(com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy(
            dao.getAllAspects().filter { subjects[it.subjectId]?.let { s -> !s.isDeleted && s.subjectType == "ASPECT" } == true }
                .associate { it.subjectId to it.parentAspectId }).isEmpty()) { "Некоректна таксономія Aspect" }
        val children = dao.getAllAspects().filter { it.parentAspectId == id && subjects[it.subjectId]?.isDeleted == false }
            .sortedWith(compareBy({ it.aspectOrder }, { it.subjectId }))
        val siblingMax = dao.getAllAspects().filter { it.parentAspectId == node.parentAspectId && subjects[it.subjectId]?.isDeleted == false }
            .maxOfOrNull { it.aspectOrder } ?: -1L
        require(siblingMax <= Long.MAX_VALUE - children.size.toLong()) { "Недостатньо діапазону порядку для дітей. Спершу впорядкуйте сусідні аспекти." }
        val refs = dao.getAllAspectOrientationRefs()
        require(refs.none { it.orientationId == id } && dao.getAllOrientationRelations().none { it.fromOrientationId == id || it.toOrientationId == id }) {
            "Aspect має несумісні типізовані залежності"
        }
        requireNoRecurrenceSource(id)
        val memberships = refs.filter { !it.isDeleted && it.aspectId == id }
        val tagRows = db.managedSubjectTagDao().getForSubject("ASPECT", id).filterNot { it.isDeleted }
        require(db.managedSubjectTagDao().getAll().none { it.subjectId == id && it.subjectType != "ASPECT" }) { "Aspect має несумісні типізовані теги" }
        val planning = db.planningReferenceDao().getAll().filter { !it.isDeleted && it.targetKind == "ASPECT" && it.targetId == id }
        planning.forEach { PlanningReferenceRole.valueOf(it.role); references.requireOwner(it.owner(), live = false) }
        AspectConversionPlan(id, subject.title, subject.version, node.parentAspectId, children.map { it.subjectId },
            memberships.size, tagRows.size, planning.size, buildMap {
                children.forEach { put("child:${it.subjectId}", subjects.getValue(it.subjectId).version) }
                memberships.forEach { put("membership:${it.id}", it.version) }
                tagRows.forEach { put("tag:${it.id}", it.version) }
                planning.forEach { put("planning:${it.id}", it.version) }
            })
    }

    suspend fun convert(plan: AspectConversionPlan, kind: OrientationKind, now: Long = System.currentTimeMillis()): String = db.withTransaction {
        require(kind in SUBJECT_CONVERSION_TARGET_KINDS) { "Цей тип має спеціалізований lifecycle" }
        val previous = db.managedSubjectConversionDao().getAll().singleOrNull { it.subjectId == plan.subjectId && it.fromType == "ASPECT" }
        if (previous != null) {
            require(previous.fromSubjectVersion == plan.version && previous.targetOrientationKind == kind.name) { "Інша конверсія вже виконана" }
            CanonicalSubjectConversionStore(db).validateCurrentClosure()
            return@withTransaction plan.subjectId
        }
        require(preview(plan.subjectId) == plan) { "Сутність або зв’язки змінилися. Відкрийте підтвердження знову." }
        val dao = db.orientationDao()
        val source = requireNotNull(dao.getManagedSubject(plan.subjectId))
        val oldAspect = requireNotNull(dao.getAspect(plan.subjectId))
        val at = maxOf(now, source.updatedAt)
        val oldTags = db.managedSubjectTagDao().getForSubject("ASPECT", source.id).filterNot { it.isDeleted }
        val oldReferences = db.planningReferenceDao().getAll().filter { !it.isDeleted && it.targetKind == "ASPECT" && it.targetId == source.id }
        val liveOwners = oldReferences.associate { it.id to runCatching { references.requireOwner(it.owner(), live = true) }.isSuccess }
        plan.childIds.forEach { aspects.move(it, plan.parentId, now = at) }
        dao.getAllAspectOrientationRefs().filter { !it.isDeleted && it.aspectId == source.id }.forEach { links.unlinkOrientation(it.id, at) }
        tags.retireForSubject(ManagedSubjectRef(ManagedSubjectType.ASPECT, source.id), at)
        oldReferences.forEach { references.remove(it.id, it.version, at) }
        dao.retireAspectSubtypeForConversion(source.id)
        val assessment = initialOrientationAssessment(kind)
        val targetVersion = Math.addExact(source.version, 1)
        orientations.saveOrientation(
            ManagedSubject(source.id, source.createdAt, at, null, false, targetVersion, ManagedSubjectType.ORIENTATION, source.title, source.description),
            OrientationNode(source.id, kind, null, ValueOrigin.UNSET, assessment),
            OrientationAssessmentRevision("${source.id}:conversion:${source.version}:assessment", at, at, null, false, 1,
                source.id, at, at, AssessmentRevisionSource.USER, "Explicit Aspect to Orientation conversion", assessment))
        val fact = ManagedSubjectConversionEntity(managedSubjectConversionId(source.id, source.version), source.id, 1, "ASPECT", "ORIENTATION",
            kind.name, source.version, targetVersion, oldAspect.parentAspectId, oldAspect.aspectOrder, oldAspect.archived, at, at, 1, null)
        CanonicalSubjectConversionStore(db).merge(listOf(fact))
        oldTags.forEach { tags.addTag(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, source.id), it.normalizedTag, at) }
        oldReferences.filter { liveOwners[it.id] == true }.forEach {
            references.attach(it.owner(), PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, source.id), PlanningReferenceRole.valueOf(it.role), at)
        }
        CanonicalSubjectConversionStore(db).validateCurrentClosure()
        requireLiveSemanticEndpoints(dao.getAllManagedSubjects(), dao.getAllOrientationRelations(), dao.getAllAspectOrientationRefs(), dao.getAllWorkspaceBindings(), dao.getAllAspects())
        source.id
    }

    private suspend fun requireNoRecurrenceSource(id: String) {
        db.canonicalRecurringSeriesDao().getAllSync().filter { !it.isDeleted && it.kind == "TASK" }.forEach {
            val source = decodeTaskRecurrenceTemplate(it.templateJson, Gson()).planningSource
            require(source?.let { it.kind.name == "ASPECT" && it.id == id } != true) {
                "Aspect є джерелом повторюваного завдання. Спершу змініть або від’єднайте джерело серії."
            }
        }
    }

    private fun PlanningReferenceEntity.owner() = PlanningReferenceOwner(PlanningReferenceOwnerKind.valueOf(ownerKind), ownerId)
}
