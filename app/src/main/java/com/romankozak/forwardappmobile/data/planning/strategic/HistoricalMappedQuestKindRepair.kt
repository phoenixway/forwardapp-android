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
    val conversions = database.managedSubjectConversionDao().getAll().mapTo(hashSetOf()) { it.subjectId }
    val directionOwners = database.workspaceDirectionEntryDao().getAll().mapNotNullTo(hashSetOf()) { it.orientationId }
    var repaired = 0

    candidates.forEach { mapping ->
        val quest = requireNotNull(quests[mapping.sourceId])
        val orientation = requireNotNull(orientations[mapping.subjectId])

        val subject = requireNotNull(subjects[mapping.subjectId]) {
            "Mapped Quest ${quest.id} has no canonical subject"
        }
        require(mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
            mapping.subjectId == LegacySubjectUuid.resolve(LegacySubjectRef(LegacyOrientationSourceType.ARC_QUEST, quest.id)) &&
            mappings.count { it.subjectId == mapping.subjectId } == 1 &&
            subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
            subject.isDeleted == quest.isDeleted && mapping.isDeleted == quest.isDeleted &&
            mapping.subjectId !in conversions && mapping.subjectId !in directionOwners &&
            subject.version < Long.MAX_VALUE && subject.updatedAt < Long.MAX_VALUE
        ) {
            "Mapped Quest ${quest.id} has additional Direction or conversion dependencies; automatic kind repair is unsafe"
        }

        orientationDao.upsertOrientations(listOf(orientation.copy(kind = OrientationKind.QUEST.name)))
        orientationDao.upsertManagedSubjects(
            listOf(subject.copy(
                updatedAt = maxOf(now, subject.updatedAt + 1L),
                syncedAt = null,
                version = subject.version + 1L,
            )),
        )
        repaired++
    }
    return repaired
}
