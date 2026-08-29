package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.database.WorkspaceDirectionEntryIssueEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.DirectionDao
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WorkspaceDirectionEntryMaterializationReport(
    val changedEntries: Int,
    val issues: List<WorkspaceDirectionEntryIssueEntity>,
)

/**
 * Materializes only the Context-backed legacy DIRECTION placement shadow.
 *
 * Legacy direction_items remain authoritative. Canonical-only entries are not
 * created, changed, or deleted by this boundary.
 */
@Singleton
class WorkspaceDirectionEntryShadowMaterializer
    @Inject
    constructor(
        private val database: AppDatabase,
        private val directionDao: DirectionDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val orientationBootstrapper: CanonicalOrientationBootstrapper,
        private val workspaceBootstrapper: CanonicalWorkspaceBootstrapper,
    ) {
        private val mutex = Mutex()

        suspend fun ensureMaterialized(
            now: Long = System.currentTimeMillis(),
        ): WorkspaceDirectionEntryMaterializationReport =
            mutex.withLock {
                /*
                 * Resolve semantic Direction provenance first, then Workspace
                 * provenance/capabilities. The entry transaction consumes only
                 * already-proven canonical dependencies.
                 */
                orientationBootstrapper.ensureBootstrapped()
                workspaceBootstrapper.ensureBootstrapped(now)

                database.withTransaction {
                    val entryDao = database.workspaceDirectionEntryDao()

                    val plan =
                        planWorkspaceDirectionEntryShadow(
                            rows = directionDao.getAllRaw(),
                            workspaces = workspaceDao.getAll(),
                            capabilities = orientationDao.getAllWorkspaceCapabilities(),
                            mappings = orientationDao.getAllLegacyMappings(),
                            subjects = orientationDao.getAllManagedSubjects(),
                            orientations = orientationDao.getAllOrientations(),
                            existingEntries = entryDao.getAll(),
                            now = now,
                        )

                    if (plan.changes.isNotEmpty()) {
                        entryDao.upsert(plan.changes)
                    }

                    val existingOpenIssues =
                        entryDao.getOpenIssues()
                            .associateBy { it.sourceDirectionItemId to it.code }

                    val issues =
                        plan.issues.map { issue ->
                            val existing =
                                existingOpenIssues[
                                    issue.sourceDirectionItemId to issue.code
                                ]
                            WorkspaceDirectionEntryIssueEntity(
                                id = stableIssueId(
                                    issue.sourceDirectionItemId,
                                    issue.code,
                                ),
                                sourceDirectionItemId =
                                    issue.sourceDirectionItemId,
                                code = issue.code,
                                detail = issue.detail,
                                createdAt = existing?.createdAt ?: now,
                                resolvedAt = null,
                            )
                        }

                    entryDao.resolveOpenIssues(now)
                    if (issues.isNotEmpty()) {
                        entryDao.upsertIssues(issues)
                    }

                    WorkspaceDirectionEntryMaterializationReport(
                        changedEntries = plan.changes.size,
                        issues = issues,
                    )
                }
            }

        private fun stableIssueId(
            sourceDirectionItemId: String,
            code: String,
        ): String =
            LegacySubjectUuid.uuidV5(
                ISSUE_NAMESPACE,
                "direction-entry-issue:$sourceDirectionItemId:$code",
            ).toString()

        companion object {
            private val ISSUE_NAMESPACE =
                UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID)
        }
    }
