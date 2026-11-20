package com.romankozak.forwardappmobile.data.planning.strategic

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectConversionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

/** Restores the required kind after an unsupported MANUAL Quest reclassification. Caller owns the transaction. */
internal suspend fun repairHistoricalMappedQuestKinds(database: AppDatabase, now: Long): Int {
    val orientationDao = database.orientationDao()
    val quests = database.arcQuestDao().getAllSync().associateBy { it.id }
    val mappings = orientationDao.getAllLegacyMappings()
    val orientations = orientationDao.getAllOrientations().associateBy { it.subjectId }
    val candidates = mappings.filter { mapping ->
        mapping.sourceType == LegacyOrientationSourceType.ARC_QUEST.name &&
            quests[mapping.sourceId]?.sourceType == ArcQuestSourceType.MANUAL.name &&
            orientations[mapping.subjectId]?.kind == OrientationKind.DIRECTION.name
    }
    if (candidates.isEmpty()) return 0

    val subjects = orientationDao.getAllManagedSubjects().associateBy { it.id }
    val currents = orientationDao.getAllAssessments().associateBy { it.orientationId }
    val revisions = orientationDao.getAllAssessmentRevisions().groupBy { it.orientationId }
    val conversions = database.managedSubjectConversionDao().getAll().groupBy { it.subjectId }
    val directionOwners = database.workspaceDirectionEntryDao().getAll().filter { it.orientationId != null }.groupBy { it.orientationId }
    val unsafe = mutableListOf<String>()
    val factsToRetire = mutableMapOf<String, String>()

    candidates.forEach { mapping ->
        val quest = requireNotNull(quests[mapping.sourceId])
        val subject = requireNotNull(subjects[mapping.subjectId]) {
            "Mapped Quest ${quest.id} has no canonical subject"
        }
        val blockers = buildList {
            if (mapping.state != LegacySubjectMappingState.CUT_OVER.name) add("mappingState=${mapping.state}")
            if (mapping.subjectId != LegacySubjectUuid.resolve(LegacySubjectRef(LegacyOrientationSourceType.ARC_QUEST, quest.id))) {
                add("nonDeterministicSubjectId")
            }
            if (mappings.count { it.subjectId == mapping.subjectId } != 1) add("multipleMappings")
            if (subject.subjectType != ManagedSubjectType.ORIENTATION.name) add("subjectType=${subject.subjectType}")
            if (subject.isDeleted != quest.isDeleted) add("subjectDeletionMismatch")
            if (mapping.isDeleted != quest.isDeleted) add("mappingDeletionMismatch")
            conversions[mapping.subjectId]?.let { facts ->
                val fact = facts.singleOrNull()
                if (fact != null && canRetireUnsupportedQuestDirectionFact(
                        fact, subject, currents[mapping.subjectId], revisions[mapping.subjectId].orEmpty(),
                    )
                ) {
                    factsToRetire[mapping.subjectId] = fact.id
                } else {
                    add("conversionHistory=${facts.joinToString("|") { item ->
                        "${item.id}:${item.fromType}/${item.fromOrientationKind}->${item.toType}/${item.targetOrientationKind}:format=${item.formatVersion}:synced=${item.syncedAt != null}"
                    }}")
                }
            }
            directionOwners[mapping.subjectId]?.let { entries ->
                add("directionEntries=${entries.joinToString("|") { entry ->
                    "${entry.id}:${entry.provenance}:deleted=${entry.isDeleted}:version=${entry.version}"
                }}")
            }
            if (subject.version == Long.MAX_VALUE || subject.updatedAt == Long.MAX_VALUE) add("subjectVersionOverflow")
        }
        if (blockers.isNotEmpty()) {
            unsafe += "${quest.id}[${blockers.joinToString()}]"
        }
    }
    require(unsafe.isEmpty()) {
        "Mapped Quest kind repair requires review: ${unsafe.joinToString("; ")}"
    }

    candidates.forEach { mapping ->
        val subject = requireNotNull(subjects[mapping.subjectId])
        val orientation = requireNotNull(orientations[mapping.subjectId])
        factsToRetire[mapping.subjectId]?.let { factId ->
            require(database.managedSubjectConversionDao().deleteUnsupportedUnsyncedQuestDirectionFact(factId) == 1) {
                "Unsupported Quest conversion changed during repair: $factId"
            }
        }
        orientationDao.upsertOrientations(listOf(orientation.copy(kind = OrientationKind.QUEST.name)))
        orientationDao.upsertManagedSubjects(
            listOf(subject.copy(
                updatedAt = maxOf(now, subject.updatedAt + 1L),
                syncedAt = null,
                version = subject.version + 1L,
            )),
        )
    }
    return candidates.size
}

private fun canRetireUnsupportedQuestDirectionFact(
    fact: ManagedSubjectConversionEntity,
    subject: ManagedSubjectEntity,
    current: OrientationAssessmentEntity?,
    revisions: List<OrientationAssessmentRevisionEntity>,
): Boolean {
    val resultId = "${fact.subjectId}:kind:${fact.fromSubjectVersion}:assessment"
    val result = revisions.singleOrNull { it.id == resultId } ?: return false
    val prior = revisions.filter { it.id != resultId }
    val priorIds = runCatching {
        JsonParser.parseString(requireNotNull(fact.previousAssessmentRevisionIdsJson))
            .asJsonArray.map { element -> element.asString }
    }.getOrNull() ?: return false
    val assessment = runCatching { decodeOrientationAssessmentJson(Gson(), result.assessmentJson) }.getOrNull()
        ?: return false

    return fact.formatVersion == 3 && fact.fromType == "ORIENTATION" && fact.toType == "ORIENTATION" &&
        fact.fromOrientationKind == OrientationKind.QUEST.name &&
        fact.targetOrientationKind == OrientationKind.DIRECTION.name &&
        fact.id == "${fact.subjectId}:ORIENTATION_KIND:${fact.fromSubjectVersion}" &&
        fact.fromSubjectVersion >= 1L && fact.fromSubjectVersion < Long.MAX_VALUE &&
        fact.toSubjectVersion == fact.fromSubjectVersion + 1L &&
        fact.version == 1L && fact.syncedAt == null && fact.createdAt == fact.updatedAt &&
        fact.previousAspectParentId == null && fact.previousAspectOrder == 0L && !fact.previousAspectArchived &&
        subject.version == fact.toSubjectVersion && subject.syncedAt == null &&
        current != null && current.revisionId == resultId && current.version == 2L &&
        !current.isDeleted && current.syncedAt == null &&
        result.orientationId == fact.subjectId && result.source == "USER" &&
        result.reason == "Explicit kind change QUEST -> DIRECTION" &&
        result.version == 1L && !result.isDeleted && result.syncedAt == null &&
        result.createdAt == fact.createdAt && result.updatedAt == fact.updatedAt &&
        prior.isNotEmpty() && prior.all { it.orientationId == fact.subjectId && it.createdAt <= fact.createdAt } &&
        priorIds.size == prior.size && priorIds.distinct().size == priorIds.size &&
        priorIds.toSet() == prior.map { it.id }.toSet() &&
        validateOrientationAssessment(OrientationKind.QUEST, assessment).isEmpty()
}
