package com.romankozak.forwardappmobile.data.planning.strategic

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
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
    val conversions = database.managedSubjectConversionDao().getAll().groupBy { it.subjectId }
    val directionOwners = database.workspaceDirectionEntryDao().getAll().filter { it.orientationId != null }.groupBy { it.orientationId }
    val unsafe = mutableListOf<String>()

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
                add("conversionHistory=${facts.joinToString("|") { fact ->
                    "${fact.id}:${fact.fromType}/${fact.fromOrientationKind}->${fact.toType}/${fact.targetOrientationKind}"
                }}")
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
