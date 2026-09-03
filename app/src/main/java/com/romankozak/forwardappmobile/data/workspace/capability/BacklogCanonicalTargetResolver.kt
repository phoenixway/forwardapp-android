package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility resolver from legacy Backlog target identity to canonical
 * BACKLOG target identity.
 *
 * This resolver never bootstraps or repairs another domain. Canonical identity
 * must already be proven before a canonical BACKLOG command is accepted.
 */
@Singleton
class BacklogCanonicalTargetResolver
    @Inject
    constructor(
        private val orientationDao: OrientationDao,
        private val workspaceDao: WorkspaceDao,
    ) {
        suspend fun resolveLegacy(
            itemType: String,
            entityId: String,
        ): WorkspaceBacklogTargetRef {
            val normalizedType = itemType.trim()
            val normalizedId = entityId.trim()
            require(normalizedType.isNotEmpty()) { "Backlog item type must not be blank" }
            require(normalizedId.isNotEmpty()) { "Backlog target id must not be blank" }

            return when (normalizedType) {
                "GOAL" -> resolveGoal(normalizedId)
                "SUBLIST", "PROJECT" -> resolveContextWorkspace(normalizedId)
                "LINK_ITEM" ->
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.LINK_ITEM,
                        normalizedId,
                    )
                "NOTE" ->
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.LEGACY_NOTE,
                        normalizedId,
                    )
                "NOTE_DOCUMENT" ->
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
                        normalizedId,
                    )
                "CHECKLIST" ->
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.CHECKLIST,
                        normalizedId,
                    )
                "MUSIC_NOTE" ->
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.MUSIC_NOTE,
                        normalizedId,
                    )
                "SCRIPT", "CONTEXT", "LINK" ->
                    error("Unsupported legacy Backlog target type $normalizedType")
                else ->
                    error("Unknown legacy Backlog target type $normalizedType")
            }
        }

        private suspend fun resolveGoal(goalId: String): WorkspaceBacklogTargetRef {
            val mapping =
                requireNotNull(
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.GOAL.name,
                        goalId,
                    ),
                ) {
                    "Goal $goalId has no canonical Orientation mapping"
                }

            require(!mapping.isDeleted) {
                "Goal $goalId canonical Orientation mapping is deleted"
            }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "Goal $goalId canonical Orientation mapping is not CUT_OVER"
            }

            return WorkspaceBacklogTargetRef(
                WorkspaceBacklogTargetKind.ORIENTATION,
                mapping.subjectId,
            )
        }

        private suspend fun resolveContextWorkspace(contextId: String): WorkspaceBacklogTargetRef {
            val workspace =
                requireNotNull(workspaceDao.getContextBackedForContextId(contextId)) {
                    "Context $contextId has no proven Context-backed Workspace"
                }

            require(!workspace.isDeleted) {
                "Context $contextId target Workspace is deleted"
            }

            return WorkspaceBacklogTargetRef(
                WorkspaceBacklogTargetKind.WORKSPACE,
                workspace.id,
            )
        }
    }
