package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextInboxSortingDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingLegacyPlanner
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingMigrationBindings
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyInboxSortingSource
import javax.inject.Inject
import javax.inject.Singleton

/** One-way compatibility boundary for pre-canonical full backups. */
@Singleton
class InboxSortingLegacyFullBackupAdapter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextInboxSortingDao: ContextInboxSortingDao,
    ) {
        suspend fun materializeStagedEvidence() {
            database.withTransaction {
                val sources =
                    contextInboxSortingDao.getAllRaw().map {
                        LegacyInboxSortingSource(
                            contextId = it.contextId,
                            rulesText = it.rulesText,
                            updatedAt = it.updatedAt,
                        )
                    }
                require(sources.all { it.updatedAt >= 0L }) {
                    "Legacy INBOX_SORTING full-backup fallback has an invalid timestamp"
                }

                val workspaces = workspaceDao.getAll()
                val workspaceGroups =
                    workspaces
                        .filter {
                            !it.isDeleted &&
                                it.provenance == CONTEXT_BACKED_PROVENANCE &&
                                !it.sourceContextId.isNullOrBlank() &&
                                it.id == it.sourceContextId
                        }
                        .groupBy { requireNotNull(it.sourceContextId) }
                require(workspaceGroups.values.none { it.size > 1 }) {
                    "Legacy INBOX_SORTING full-backup fallback has ambiguous Workspace ownership"
                }

                val capabilities = database.orientationDao().getAllWorkspaceCapabilities()
                val sortingCapabilities =
                    capabilities.filter {
                        it.capabilityType == INBOX_SORTING_CAPABILITY_TYPE &&
                            it.instanceKey == DEFAULT_CAPABILITY_INSTANCE_KEY
                    }
                val capabilityGroups = sortingCapabilities.groupBy { it.workspaceId }
                require(capabilityGroups.values.none { it.size > 1 }) {
                    "Legacy INBOX_SORTING full-backup fallback has duplicate capability instances"
                }

                val plan =
                    InboxSortingLegacyPlanner.plan(
                        sources = sources,
                        bindings =
                            InboxSortingMigrationBindings(
                                workspaceIdByContextId =
                                    workspaceGroups.mapValues { (_, matches) -> matches.single().id },
                                capabilityInstanceIdByWorkspaceId =
                                    capabilityGroups.mapValues { (_, matches) -> matches.single().id },
                            ),
                    )
                require(plan.isFullyAccounted) {
                    buildString {
                        append("Legacy INBOX_SORTING full-backup fallback was rejected")
                        plan.issues.forEach { issue ->
                            append("\n${issue.code}: context=${issue.contextId} line=${issue.lineNumber}: ${issue.detail}")
                        }
                    }
                }

                val byId = sortingCapabilities.associateBy { it.id }
                val changes =
                    plan.updates.mapNotNull { update ->
                        val current = requireNotNull(byId[update.capabilityInstanceId])
                        current.configurationUpdate(
                            configurationVersion = update.configurationVersion,
                            configuration = update.configuration,
                            updatedAt = maxOf(current.updatedAt, update.sourceUpdatedAt),
                        )
                    }
                if (changes.isNotEmpty()) {
                    database.orientationDao().upsertWorkspaceCapabilities(changes)
                }
                contextInboxSortingDao.deleteAll()
            }
        }
    }

private fun WorkspaceCapabilityInstanceEntity.configurationUpdate(
    configurationVersion: Int,
    configuration: String,
    updatedAt: Long,
): WorkspaceCapabilityInstanceEntity? =
    if (
        this.configurationVersion == configurationVersion &&
        this.configuration == configuration
    ) {
        null
    } else {
        copy(
            configurationVersion = configurationVersion,
            configuration = configuration,
            updatedAt = updatedAt,
            syncedAt = null,
            version = version + 1L,
        )
    }

private const val CONTEXT_BACKED_PROVENANCE = "CONTEXT_BACKED"
private const val INBOX_SORTING_CAPABILITY_TYPE = "INBOX_SORTING"
