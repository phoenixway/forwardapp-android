package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Context-shaped UI locator adapter for canonical EXECUTION_LOG ownership.
 *
 * Local persisted rows are canonicalized by MIGRATION_188_189. Runtime code
 * uses this boundary only to resolve a legacy Context locator to its proven
 * CONTEXT_BACKED Workspace owner.
 */
@Singleton
class ExecutionLogWorkspaceOwnershipBridge
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
    ) {
        suspend fun resolveContextBackedWorkspaceId(contextId: String): String? =
            workspaceDao.getContextBackedForContextId(contextId)?.id
    }
