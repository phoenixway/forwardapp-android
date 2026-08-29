package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.sync.SyncRepository
import timber.log.Timber
import javax.inject.Inject

class SelectiveImportCoordinator @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend fun importSelection(state: SelectiveImportState): Result<Unit> {
        val content = state.backupContent
            ?: return Result.failure(IllegalStateException("Nothing to import"))
        val selection = PreparedSelection.from(content)

        Timber.tag("IMPORT_DEBUG").d("Total projects selected: ${selection.selectedProjects.size}")
        Timber
            .tag("IMPORT_DEBUG")
            .d("Projects with parents: ${selection.selectedProjects.count { it.parentId != null }}")
        Timber
            .tag("IMPORT_DEBUG")
            .d("Root projects (no parent): ${selection.selectedProjects.count { it.parentId == null }}")
        Timber
            .tag("IMPORT_DEBUG")
            .d(
                "Regular (non-system) projects: ${selection.regularProjects.size}, " +
                    "System projects: ${selection.selectedProjects.size - selection.regularProjects.size}",
            )

        val snapshotBundle =
            state.sourceSnapshotBundle
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Selective import requires a canonical SnapshotBundle source.",
                    ),
                )

        return importSnapshotSelection(
            snapshotBundle = snapshotBundle,
            selection = selection,
            sharedSelection = state.selection,
        )
    }

    private suspend fun importSnapshotSelection(
        snapshotBundle: SnapshotBundle,
        selection: PreparedSelection,
        sharedSelection: WorkspaceSelectiveImportSelection,
    ): Result<Unit> {
        val filteredSnapshotBundle =
            syncRepository.filterSnapshotBundleForSelectiveImport(
                bundle = snapshotBundle,
                selection = sharedSelection.takeUnless { it.isEmpty() } ?: selection.snapshotSelection,
            )

        return syncRepository.importSelectedSnapshotBundle(filteredSnapshotBundle).map { Unit }
    }

    private data class PreparedSelection(
        val selectedProjects: List<Context>,
        val regularProjects: List<Context>,
        val projectsWithValidParents: List<Context>,
        val selectedGoals: List<Goal>,
        val selectedLegacyNotes: List<LegacyNoteEntity>,
        val selectedActivityRecords: List<ActivityRecord>,
        val selectedBacklogItems: List<BacklogItem>,
        val selectedBacklogOrdersFiltered: List<BacklogOrder>,
        val selectedDocuments: List<NoteDocumentEntity>,
        val selectedChecklists: List<ChecklistEntity>,
        val filteredChecklistItems: List<ChecklistItemEntity>,
        val selectedLinkItems: List<LinkItemEntity>,
        val selectedInboxRecords: List<InboxRecord>,
        val selectedContextLogs: List<ContextLog>,
        val selectedScripts: List<ScriptEntity>,
        val filteredScripts: List<ScriptEntity>,
        val selectedAttachments: List<AttachmentEntity>,
        val filteredCrossRefs: List<ContextAttachmentCrossRef>,
        val filteredListItems: List<BacklogItem>,
        val snapshotSelection: WorkspaceSelectiveImportSelection,
    ) {
        companion object {
            fun from(content: SelectableDatabaseContent): PreparedSelection {
                val rawSelection = RawSelection.from(content)
                val projectSelection = buildProjectSelection(content, rawSelection.selectedProjects)
                val legacySelection = buildLegacySelection(content, rawSelection, projectSelection.selectedContextIds)
                val snapshotSelection = buildSnapshotSelection(rawSelection)

                return PreparedSelection(
                    selectedProjects = rawSelection.selectedProjects,
                    regularProjects = projectSelection.regularProjects,
                    projectsWithValidParents = projectSelection.projectsWithValidParents,
                    selectedGoals = rawSelection.selectedGoals,
                    selectedLegacyNotes = rawSelection.selectedLegacyNotes,
                    selectedActivityRecords = rawSelection.selectedActivityRecords,
                    selectedBacklogItems = rawSelection.selectedBacklogItems,
                    selectedBacklogOrdersFiltered = legacySelection.selectedBacklogOrdersFiltered,
                    selectedDocuments = rawSelection.selectedDocuments,
                    selectedChecklists = rawSelection.selectedChecklists,
                    filteredChecklistItems = legacySelection.filteredChecklistItems,
                    selectedLinkItems = rawSelection.selectedLinkItems,
                    selectedInboxRecords = rawSelection.selectedInboxRecords,
                    selectedContextLogs = rawSelection.selectedContextLogs,
                    selectedScripts = rawSelection.selectedScripts,
                    filteredScripts = legacySelection.filteredScripts,
                    selectedAttachments = rawSelection.selectedAttachments,
                    filteredCrossRefs = legacySelection.filteredCrossRefs,
                    filteredListItems = legacySelection.filteredListItems,
                    snapshotSelection = snapshotSelection,
                )
            }

            private fun buildProjectSelection(
                content: SelectableDatabaseContent,
                selectedProjects: List<Context>,
            ): ProjectSelection {
                val regularProjects = selectedProjects.filterNot { SystemContexts.isSystem(ContextId(it.id)) }
                val allProjectsMap = content.projects.map { it.item }.associateBy { it.id }
                val regularContextIds = regularProjects.map { it.id }.toSet()
                val projectsWithValidParents =
                    regularProjects.filter { context ->
                        isProjectValidForImport(
                            contextId = context.id,
                            allProjectsMap = allProjectsMap,
                            regularContextIds = regularContextIds,
                        )
                    }

                return ProjectSelection(
                    regularProjects = regularProjects,
                    projectsWithValidParents = projectsWithValidParents,
                    selectedContextIds = projectsWithValidParents.map { it.id }.toSet(),
                )
            }

            private fun buildLegacySelection(
                content: SelectableDatabaseContent,
                rawSelection: RawSelection,
                selectedContextIds: Set<String>,
            ): LegacySelection {
                val selectedGoalIds = rawSelection.selectedGoals.map { it.id }.toSet()
                val selectedLegacyNoteIds = rawSelection.selectedLegacyNotes.map { it.id }.toSet()
                val selectedDocumentIds = rawSelection.selectedDocuments.map { it.id }.toSet()
                val selectedChecklistIds = rawSelection.selectedChecklists.map { it.id }.toSet()
                val selectedScriptIds = rawSelection.selectedScripts.map { it.id }.toSet()
                val selectedInboxRecordIds = rawSelection.selectedInboxRecords.map { it.id }.toSet()
                val selectedAttachmentIds = rawSelection.selectedAttachments.map { it.id }.toSet()

                val selectedBacklogOrdersFiltered =
                    rawSelection.selectedBacklogOrders.filter { order ->
                        order.listId in selectedContextIds && order.itemId in (selectedContextIds + selectedGoalIds)
                    }
                val filteredListItems =
                    content.backlogItems.map { it.item }.filter { listItem ->
                        listItem.contextId in selectedContextIds ||
                            listItem.entityId in selectedGoalIds ||
                            listItem.entityId in selectedLegacyNoteIds ||
                            listItem.entityId in selectedDocumentIds ||
                            listItem.entityId in selectedChecklistIds ||
                            listItem.entityId in selectedScriptIds ||
                            listItem.entityId in selectedInboxRecordIds
                    }
                val filteredChecklistItems =
                    content.checklistItems.map { it.item }.filter { it.checklistId in selectedChecklistIds }
                val filteredCrossRefs =
                    content.allContextAttachmentCrossRefs.filter { crossRef ->
                        crossRef.contextId in selectedContextIds && crossRef.attachmentId in selectedAttachmentIds
                    }
                val filteredScripts =
                    rawSelection.selectedScripts.filter { script ->
                        script.contextId == null || script.contextId in selectedContextIds
                    }

                return LegacySelection(
                    selectedBacklogOrdersFiltered = selectedBacklogOrdersFiltered,
                    filteredListItems = filteredListItems,
                    filteredChecklistItems = filteredChecklistItems,
                    filteredCrossRefs = filteredCrossRefs,
                    filteredScripts = filteredScripts,
                )
            }

            private fun buildSnapshotSelection(rawSelection: RawSelection): WorkspaceSelectiveImportSelection =
                WorkspaceSelectiveImportSelection(
                    selectedContextIds = rawSelection.selectedProjects.map { it.id }.toSet(),
                    selectedGoalIds = rawSelection.selectedGoals.map { it.id }.toSet(),
                    selectedBacklogItemIds = rawSelection.selectedBacklogItems.map { it.id }.toSet(),
                    selectedDocumentIds = rawSelection.selectedDocuments.map { it.id }.toSet(),
                    selectedChecklistIds = rawSelection.selectedChecklists.map { it.id }.toSet(),
                    selectedLinkItemIds = rawSelection.selectedLinkItems.map { it.id }.toSet(),
                    selectedInboxRecordIds = rawSelection.selectedInboxRecords.map { it.id }.toSet(),
                    selectedContextLogIds = rawSelection.selectedContextLogs.map { it.id }.toSet(),
                    selectedScriptIds = rawSelection.selectedScripts.map { it.id }.toSet(),
                    selectedAttachmentIds = rawSelection.selectedAttachments.map { it.id }.toSet(),
                    selectedActivityRecordIds = rawSelection.selectedActivityRecords.map { it.id }.toSet(),
                )

            private fun isProjectValidForImport(
                contextId: String,
                allProjectsMap: Map<String, Context>,
                regularContextIds: Set<String>,
                visited: Set<String> = emptySet(),
            ): Boolean {
                val isVisited = contextId in visited
                val project = allProjectsMap[contextId]
                val parentProject = project?.parentId?.let(allProjectsMap::get)
                val parentId = project?.parentId

                return when {
                    isVisited -> false
                    project == null -> false
                    parentId == null -> true
                    parentProject == null -> false
                    SystemContexts.isSystem(ContextId(parentProject.id)) -> true
                    else -> {
                        parentId in regularContextIds &&
                            isProjectValidForImport(
                                contextId = parentId,
                                allProjectsMap = allProjectsMap,
                                regularContextIds = regularContextIds,
                                visited = visited + contextId,
                            )
                    }
                }
            }
        }
    }

    private data class RawSelection(
        val selectedProjects: List<Context>,
        val selectedGoals: List<Goal>,
        val selectedLegacyNotes: List<LegacyNoteEntity>,
        val selectedActivityRecords: List<ActivityRecord>,
        val selectedBacklogItems: List<BacklogItem>,
        val selectedBacklogOrders: List<BacklogOrder>,
        val selectedDocuments: List<NoteDocumentEntity>,
        val selectedChecklists: List<ChecklistEntity>,
        val selectedLinkItems: List<LinkItemEntity>,
        val selectedInboxRecords: List<InboxRecord>,
        val selectedContextLogs: List<ContextLog>,
        val selectedScripts: List<ScriptEntity>,
        val selectedAttachments: List<AttachmentEntity>,
    ) {
        companion object {
            fun from(content: SelectableDatabaseContent): RawSelection =
                RawSelection(
                    selectedProjects = content.projects.selectedItems(),
                    selectedGoals = content.goals.selectedItems(),
                    selectedLegacyNotes = content.legacyNotes.selectedItems(),
                    selectedActivityRecords = content.activityRecords.selectedItems(),
                    selectedBacklogItems = content.backlogItems.selectedItems(),
                    selectedBacklogOrders = content.backlogOrders.selectedItems(),
                    selectedDocuments = content.documents.selectedItems(),
                    selectedChecklists = content.checklists.selectedItems(),
                    selectedLinkItems = content.linkItems.selectedItems(),
                    selectedInboxRecords = content.inboxRecords.selectedItems(),
                    selectedContextLogs = content.contextLogs.selectedItems(),
                    selectedScripts = content.scripts.selectedItems(),
                    selectedAttachments = content.attachments.selectedItems(),
                )
        }
    }

    private data class ProjectSelection(
        val regularProjects: List<Context>,
        val projectsWithValidParents: List<Context>,
        val selectedContextIds: Set<String>,
    )

    private data class LegacySelection(
        val selectedBacklogOrdersFiltered: List<BacklogOrder>,
        val filteredListItems: List<BacklogItem>,
        val filteredChecklistItems: List<ChecklistItemEntity>,
        val filteredCrossRefs: List<ContextAttachmentCrossRef>,
        val filteredScripts: List<ScriptEntity>,
    )
}

private fun <T> List<SelectableDiffItem<T>>.selectedItems(): List<T> =
    filter { it.isSelected && it.isSelectable }.map { it.item }

private fun WorkspaceSelectiveImportSelection.isEmpty(): Boolean =
    selectedContextIds.isEmpty() &&
        selectedGoalIds.isEmpty() &&
        selectedBacklogItemIds.isEmpty() &&
        selectedDocumentIds.isEmpty() &&
        selectedChecklistIds.isEmpty() &&
        selectedLinkItemIds.isEmpty() &&
        selectedInboxRecordIds.isEmpty() &&
        selectedContextLogIds.isEmpty() &&
        selectedScriptIds.isEmpty() &&
        selectedAttachmentIds.isEmpty() &&
        selectedActivityRecordIds.isEmpty()
