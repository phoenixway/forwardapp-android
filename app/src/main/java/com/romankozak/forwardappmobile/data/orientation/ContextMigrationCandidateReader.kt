package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import javax.inject.Inject
import javax.inject.Singleton

data class ContextMigrationCandidate(
    val id: String,
    val title: String,
)

/** Read-only picker projection; final adoption eligibility stays in migrateContext(). */
@Singleton
class ContextMigrationCandidateReader
    @Inject
    constructor(
        private val orientationDao: OrientationDao,
    ) {
        suspend fun existingAspectCandidates(): List<ContextMigrationCandidate> =
            candidates(ManagedSubjectType.ASPECT, orientationDao.getAllAspects().mapTo(hashSetOf()) { it.subjectId })

        suspend fun existingOrientationCandidates(): List<ContextMigrationCandidate> =
            candidates(ManagedSubjectType.ORIENTATION, orientationDao.getAllOrientations().mapTo(hashSetOf()) { it.subjectId })

        private suspend fun candidates(
            type: ManagedSubjectType,
            nodeIds: Set<String>,
        ): List<ContextMigrationCandidate> =
            orientationDao.getAllManagedSubjects()
                .asSequence()
                .filter { !it.isDeleted && it.subjectType == type.name && it.id in nodeIds }
                .map { ContextMigrationCandidate(id = it.id, title = it.title) }
                .sortedWith(compareBy<ContextMigrationCandidate> { it.title }.thenBy { it.id })
                .toList()
    }
