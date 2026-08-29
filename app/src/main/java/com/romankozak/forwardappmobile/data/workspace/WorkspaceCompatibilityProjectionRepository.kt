package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.database.WorkspaceBootstrapIssueEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

data class WorkspaceCompatibilityProjection(
    val workspace: WorkspaceEntity,
    val sourceContext: Context?,
    val capabilities: List<WorkspaceCapabilityInstanceEntity>,
    val diagnostics: List<WorkspaceBootstrapIssueEntity>,
)

/** Read-only inspection boundary. It does not redirect existing Context reads or writes. */
@Singleton
class WorkspaceCompatibilityProjectionRepository
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val contextDao: ContextDao,
    ) {
        fun observeWorkspaces(): Flow<List<WorkspaceEntity>> = workspaceDao.observeAll()

        fun observeDiagnostics(): Flow<List<WorkspaceBootstrapIssueEntity>> =
            workspaceDao.observeOpenBootstrapIssues()

        suspend fun get(workspaceId: String): WorkspaceCompatibilityProjection? {
            val workspace = workspaceDao.getById(workspaceId) ?: return null
            return WorkspaceCompatibilityProjection(
                workspace = workspace,
                sourceContext = contextDao.getContextById(workspaceId),
                capabilities =
                    orientationDao.getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == workspaceId && !it.isDeleted }
                        .sortedBy { it.capabilityOrder },
                diagnostics =
                    workspaceDao.getOpenBootstrapIssues()
                        .filter { it.contextId == workspaceId },
            )
        }
    }
