package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalOrientationSyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: OrientationDao,
        private val bootstrapper: CanonicalOrientationBootstrapper,
    ) {
        suspend fun loadUnsynced(): CanonicalOrientationSyncPayload {
            bootstrapper.ensureBootstrapped()
            return database.withTransaction {
                val payload = loadFullPayload()
                if (payload.hasDirtyRows()) payload else CanonicalOrientationSyncPayload()
            }
        }

        suspend fun markSynced(ack: CanonicalOrientationSyncAck) {
            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                ack.managedSubjects.forEach { dao.markSubjectSynced(it.id, it.version, syncedAt) }
                ack.assessments.forEach { dao.markAssessmentSynced(it.id, it.version, syncedAt) }
                ack.assessmentRevisions.forEach { dao.markAssessmentRevisionSynced(it.id, it.version, syncedAt) }
                ack.legacyMappings.forEach { dao.markLegacyMappingSynced(it.id, it.version, syncedAt) }
                ack.relations.forEach { dao.markRelationSynced(it.id, it.version, syncedAt) }
                ack.aspectRefs.forEach { dao.markAspectRefSynced(it.id, it.version, syncedAt) }
                ack.workspaceBindings.forEach { dao.markWorkspaceBindingSynced(it.id, it.version, syncedAt) }
                ack.workspaceCapabilities.forEach { dao.markWorkspaceCapabilitySynced(it.id, it.version, syncedAt) }
                ack.savedViews.forEach { dao.markSavedViewSynced(it.id, it.version, syncedAt) }
            }
        }

        private suspend fun loadFullPayload() =
            CanonicalOrientationSyncPayload(
                managedSubjects = dao.getAllManagedSubjects(),
                orientations = dao.getAllOrientations(),
                aspects = dao.getAllAspects(),
                assessments = dao.getAllAssessments(),
                assessmentRevisions = dao.getAllAssessmentRevisions(),
                legacyMappings = dao.getAllLegacyMappings(),
                relations = dao.getAllOrientationRelations(),
                aspectRefs = dao.getAllAspectOrientationRefs(),
                workspaceBindings = dao.getAllWorkspaceBindings(),
                workspaceCapabilities = dao.getAllWorkspaceCapabilities(),
                savedViews = dao.getAllSavedViews(),
            )
    }

private fun CanonicalOrientationSyncPayload.hasDirtyRows(): Boolean =
    managedSubjects.any { it.syncedAt == null } ||
        assessments.any { it.syncedAt == null } ||
        assessmentRevisions.any { it.syncedAt == null } ||
        legacyMappings.any { it.syncedAt == null } ||
        relations.any { it.syncedAt == null } ||
        aspectRefs.any { it.syncedAt == null } ||
        workspaceBindings.any { it.syncedAt == null } ||
        workspaceCapabilities.any { it.syncedAt == null } ||
        savedViews.any { it.syncedAt == null }
