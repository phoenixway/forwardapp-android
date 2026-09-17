package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Final reserved-System Context compatibility-shell retirement boundary.
 *
 * This is deliberately a runtime convergence step rather than a Room schema
 * migration. Older databases may reach the current schema before
 * SystemWorkspaceMaterializer has promoted all reserved owners, so deleting
 * shells during schema migration would discard the only bounded historical
 * metadata evidence before canonical convergence can consume it.
 *
 * Preconditions are checked for all exact current reserved identities before
 * any Context row is deleted:
 *
 * - the reserved definition set is exactly 20 distinct ids;
 * - every same-id Workspace is live CANONICAL_ONLY;
 * - every same-id Workspace has sourceContextId == null;
 * - every canonical System tag collection has been established.
 *
 * Once those invariants hold, only live exact-reserved Context rows are
 * physically deleted. Reserved tombstones, ordinary Contexts, and historical
 * non-reserved sys_* Contexts are intentionally untouched.
 */
@Singleton
class SystemContextShellRetirer
    @Inject
    constructor(
        private val database: AppDatabase,
        private val contextDao: ContextDao,
    ) {
        data class Result(
            val deletedActiveShells: Int,
        )

        suspend fun retireActiveReservedShells(): Result =
            database.withTransaction {
                val definitions = SystemOperationalDefinitions.all
                require(definitions.size == 20) {
                    "Reserved System shell retirement requires exactly 20 definitions; " +
                        "found ${definitions.size}"
                }

                val reservedIds = definitions.map { it.id }
                require(reservedIds.distinct().size == 20) {
                    "Reserved System shell retirement definitions contain duplicate ids"
                }

                val workspacesById =
                    database.workspaceDao()
                        .getAll()
                        .associateBy { it.id }

                val tagSeedStatesById =
                    database.systemWorkspaceTagSeedStateDao()
                        .getAll()
                        .associateBy { it.workspaceId }

                definitions.forEach { definition ->
                    val workspace =
                        requireNotNull(workspacesById[definition.id]) {
                            "Missing reserved System Workspace before shell retirement: ${definition.id}"
                        }

                    require(
                        !workspace.isDeleted &&
                            workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                            workspace.sourceContextId == null,
                    ) {
                        "Reserved System Workspace is not a live CANONICAL_ONLY owner " +
                            "before shell retirement: ${definition.id}"
                    }

                    require(tagSeedStatesById[definition.id] != null) {
                        "Reserved System Workspace tag collection is not established " +
                            "before shell retirement: ${definition.id}"
                    }
                }

                val contextsById =
                    contextDao.getContextsByIds(reservedIds)
                        .associateBy { it.id }

                var deleted = 0
                definitions.forEach { definition ->
                    val context = contextsById[definition.id]
                    if (context != null && !context.isDeleted) {
                        contextDao.deleteContextById(definition.id)
                        deleted += 1
                    }
                }

                Result(deletedActiveShells = deleted)
            }
    }
