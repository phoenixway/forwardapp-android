package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectConversionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.requireValidConversionShape
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.toConversionEvidence
import com.romankozak.forwardappmobile.database.AppDatabase
import javax.inject.Inject

/** Durable facts only. This is not the cross-type mutation command. */
class CanonicalSubjectConversionStore @Inject constructor(private val db: AppDatabase) {
    suspend fun merge(incoming: List<ManagedSubjectConversionEntity>?) {
        if (incoming == null) return
        db.withTransaction {
            require(incoming.map { it.id }.distinct().size == incoming.size) { "Duplicate semantic conversion identity" }
            incoming.forEach { it.requireValidConversionShape() }
            val current = db.managedSubjectConversionDao().getAll().associateBy { it.id }
            val prospective = current.toMutableMap()
            incoming.forEach { row ->
                val previous = current[row.id]
                require(previous == null || previous.copy(syncedAt = null) == row.copy(syncedAt = null)) {
                    "Divergent immutable semantic conversion: ${row.id}"
                }
                prospective[row.id] = row
            }
            validate(prospective.values.toList())
            incoming.filter { it.id !in current }.forEach { db.managedSubjectConversionDao().insert(it.copy(syncedAt = null)) }
        }
    }

    suspend fun restore(incoming: List<ManagedSubjectConversionEntity>) = db.withTransaction {
        validate(incoming)
        db.managedSubjectConversionDao().clearForExactRestore()
        incoming.forEach { db.managedSubjectConversionDao().insert(it) }
    }

    internal suspend fun formerAspectIds(): Set<String> {
        val rows = db.managedSubjectConversionDao().getAll()
        validate(rows)
        return rows.filter { it.fromType == "ASPECT" }.mapTo(hashSetOf()) { it.subjectId }
    }

    /** Called after all incoming relation families, not midway through their transaction. */
    suspend fun validateCurrentClosure() {
        val ids = formerAspectIds()
        if (db.managedSubjectConversionDao().getAll().isEmpty()) return
        require(db.managedSubjectTagDao().getAll().none { !it.isDeleted && it.subjectType == "ASPECT" && it.subjectId in ids }) { "Live former-type semantic tag" }
        require(db.planningReferenceDao().getAll().none { !it.isDeleted && it.targetKind == "ASPECT" && it.targetId in ids }) { "Live former-type Planning reference" }
        require(db.orientationDao().getAllAspectOrientationRefs().none { !it.isDeleted && it.aspectId in ids }) { "Live former-type Aspect membership" }
        val liveSubjects = db.orientationDao().getAllManagedSubjects().filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.id }
        require(db.orientationDao().getAllAspects().none { it.subjectId in liveSubjects && it.parentAspectId in ids }) { "Unpromoted Aspect child" }
        db.canonicalRecurringSeriesDao().getAllSync().filter { !it.isDeleted && it.kind == "TASK" }.forEach {
            val source = com.romankozak.forwardappmobile.data.recurrence.decodeTaskRecurrenceTemplate(it.templateJson, com.google.gson.Gson()).planningSource
            require(source?.let { it.kind.name == "ASPECT" && it.id in ids } != true) { "Unconverted recurrence source dependency" }
        }
        val dao = db.orientationDao()
        validateCanonicalPayloadReferences(com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle(
            managedSubjects = dao.getAllManagedSubjects(), orientations = dao.getAllOrientations(), aspects = dao.getAllAspects(),
            orientationAssessments = dao.getAllAssessments(), orientationAssessmentRevisions = dao.getAllAssessmentRevisions(),
            orientationScoringOverrides = dao.getAllScoringOverrides(), legacySubjectMappings = dao.getAllLegacyMappings(),
            orientationRelations = dao.getAllOrientationRelations(), aspectOrientationRefs = dao.getAllAspectOrientationRefs(),
            workspaceBindings = dao.getAllWorkspaceBindings(), workspaceCapabilityInstances = dao.getAllWorkspaceCapabilities(),
            savedOrientationViews = dao.getAllSavedViews(), managedSubjectConversions = db.managedSubjectConversionDao().getAll(),
        ), validateEmbeddedWorkspaceTopology = false)
    }

    private suspend fun validate(rows: List<ManagedSubjectConversionEntity>) {
        require(rows.map { it.id }.distinct().size == rows.size) { "Duplicate semantic conversion identity" }
        val subjects = db.orientationDao().getAllManagedSubjects().associateBy { it.id }
        val orientations = db.orientationDao().getAllOrientations().associateBy { it.subjectId }
        val aspects = db.orientationDao().getAllAspects().mapTo(hashSetOf()) { it.subjectId }
        val legacy = db.orientationDao().getAllLegacyMappings().mapTo(hashSetOf()) { it.subjectId }
        val revisions = db.orientationDao().getAllAssessmentRevisions().associateBy { it.id }
        rows.groupBy { it.subjectId }.forEach { (id, facts) ->
            val subject = requireNotNull(subjects[id]) { "Missing converted subject" }
            require(subject.subjectType == "ORIENTATION" && id !in aspects && id !in legacy) { "Invalid converted native subject" }
            val kind = com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind.valueOf(requireNotNull(orientations[id]).kind)
            val historical = com.romankozak.forwardappmobile.shared.core.domain.orientation.historicalAssessmentKinds(facts.map { it.toConversionEvidence() }, kind, subject.version)
            historical.forEach { (revisionId, historicalKind) ->
                val revision = requireNotNull(revisions[revisionId]) { "Missing historical assessment" }
                require(revision.orientationId == id)
                require(com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment(historicalKind,
                    com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson(com.google.gson.Gson(), revision.assessmentJson)).isEmpty())
            }
        }
    }
}
