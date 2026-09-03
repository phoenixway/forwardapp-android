package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility projection for the post-cutover Context Backlog runtime.
 *
 * Canonical workspace_backlog_entries remain the explicit placement authority.
 * This reader recreates the historical BacklogItem DTO expected by existing UI
 * and runtime consumers without reading the retired legacy explicit-placement
 * authority.
 *
 * Hashtag Goal appearances remain a separate rebuildable local projection.
 */
@Singleton
class CanonicalBacklogCompatibilityReader
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        fun observeItemsForContext(contextId: String): Flow<List<BacklogItem>> =
            combine(
                database.workspaceBacklogEntryDao().observeLive(contextId),
                database.backlogGoalAssociationLinkDao().observeForContext(contextId),
                database.orientationDao().observeLegacyMappings(),
                database.workspaceDao().observeAll(),
            ) { entries, projections, mappings, workspaces ->
                val support = ProjectionSupport(mappings, workspaces)
                (
                    entries.map { entry -> entry.toCompatibilityItem(support) } +
                        projections.map(BacklogGoalAssociationLink::toCompatibilityItem)
                ).sortedWith(compareBy<BacklogItem> { it.order }.thenBy { it.id })
            }

        suspend fun getDirectItemsForContext(contextId: String): List<BacklogItem> {
            val support = snapshotSupport()
            return database.workspaceBacklogEntryDao()
                .getLive(contextId)
                .map { entry -> entry.toCompatibilityItem(support) }
                .sortedWith(compareBy<BacklogItem> { it.order }.thenBy { it.id })
        }

        suspend fun getItemsForContext(contextId: String): List<BacklogItem> {
            val support = snapshotSupport()
            val explicit =
                database.workspaceBacklogEntryDao()
                    .getLive(contextId)
                    .map { entry -> entry.toCompatibilityItem(support) }
            val derived =
                database.backlogGoalAssociationLinkDao()
                    .getForContext(contextId)
                    .map(BacklogGoalAssociationLink::toCompatibilityItem)
            return (explicit + derived)
                .sortedWith(compareBy<BacklogItem> { it.order }.thenBy { it.id })
        }

        suspend fun getItemsByIds(ids: Collection<String>): List<BacklogItem> {
            val requested = ids.map(String::trim).filter(String::isNotEmpty).distinct()
            if (requested.isEmpty()) return emptyList()

            val support = snapshotSupport()
            val explicit =
                database.workspaceBacklogEntryDao()
                    .getByIds(requested)
                    .map { entry -> entry.toCompatibilityItem(support) }
            val derived =
                database.backlogGoalAssociationLinkDao()
                    .getByProjectionIds(requested)
                    .map(BacklogGoalAssociationLink::toCompatibilityItem)

            val byId = (explicit + derived).associateBy { it.id }
            return requested.mapNotNull(byId::get)
        }

        private suspend fun snapshotSupport(): ProjectionSupport =
            ProjectionSupport(
                mappings = database.orientationDao().getAllLegacyMappings(),
                workspaces = database.workspaceDao().getAll(),
            )
    }

private class ProjectionSupport(
    mappings: List<LegacySubjectMappingEntity>,
    workspaces: List<WorkspaceEntity>,
) {
    val goalIdByOrientationId: Map<String, String> =
        mappings
            .asSequence()
            .filter { mapping ->
                mapping.sourceType == GOAL_SOURCE_TYPE &&
                    mapping.state == CUT_OVER_MAPPING_STATE &&
                    !mapping.isDeleted
            }
            .associate { mapping -> mapping.subjectId to mapping.sourceId }

    val workspacesById: Map<String, WorkspaceEntity> = workspaces.associateBy { it.id }
}

private fun WorkspaceBacklogEntryEntity.toCompatibilityItem(
    support: ProjectionSupport,
): BacklogItem {
    val targetKind =
        runCatching { WorkspaceBacklogTargetKind.valueOf(targetKind) }
            .getOrElse {
                error("Unsupported canonical BACKLOG target kind $targetKind for entry $id")
            }

    val compatibilityTarget =
        when (targetKind) {
            WorkspaceBacklogTargetKind.ORIENTATION -> {
                val goalId =
                    requireNotNull(support.goalIdByOrientationId[targetId]) {
                        "Canonical BACKLOG Orientation $targetId has no live CUT_OVER GOAL mapping"
                    }
                BacklogItemTypeValues.GOAL to goalId
            }

            WorkspaceBacklogTargetKind.WORKSPACE -> {
                val targetWorkspace =
                    requireNotNull(support.workspacesById[targetId]) {
                        "Canonical BACKLOG Workspace target $targetId does not exist"
                    }
                require(targetWorkspace.provenance == WorkspaceProvenance.CONTEXT_BACKED.name) {
                    "Canonical-only Workspace $targetId cannot be represented by legacy BacklogItem"
                }
                val sourceContextId =
                    requireNotNull(targetWorkspace.sourceContextId?.takeIf(String::isNotBlank)) {
                        "Context-backed Workspace $targetId has no source Context id"
                    }

                // PROJECT was an undeclared historical alias. Canonical WORKSPACE
                // identity intentionally erases that spelling distinction.
                BacklogItemTypeValues.SUBLIST to sourceContextId
            }

            WorkspaceBacklogTargetKind.LINK_ITEM ->
                BacklogItemTypeValues.LINK_ITEM to targetId

            WorkspaceBacklogTargetKind.LEGACY_NOTE ->
                BacklogItemTypeValues.NOTE to targetId

            WorkspaceBacklogTargetKind.NOTE_DOCUMENT ->
                BacklogItemTypeValues.NOTE_DOCUMENT to targetId

            WorkspaceBacklogTargetKind.CHECKLIST ->
                BacklogItemTypeValues.CHECKLIST to targetId

            WorkspaceBacklogTargetKind.MUSIC_NOTE ->
                BacklogItemTypeValues.MUSIC_NOTE to targetId
        }

    return BacklogItem(
        id = id,
        contextId = workspaceId,
        itemType = compatibilityTarget.first,
        entityId = compatibilityTarget.second,
        order = entryOrder,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )
}

private fun BacklogGoalAssociationLink.toCompatibilityItem(): BacklogItem =
    BacklogItem(
        id = projectionId,
        contextId = contextId,
        itemType = BacklogItemTypeValues.GOAL,
        entityId = goalId,
        associationOwnerContextId = ownerContextId,
        associationTag = associationTag,
        order = order,
        updatedAt = linkedAt,
        syncedAt = null,
        isDeleted = false,
        version = 0L,
    )

private const val GOAL_SOURCE_TYPE = "GOAL"
private const val CUT_OVER_MAPPING_STATE = "CUT_OVER"
