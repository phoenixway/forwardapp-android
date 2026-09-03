package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
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
import com.romankozak.forwardappmobile.core.data.models.sync.BackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.DiffResult
import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.UpdatedItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportSourceMode
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewModel
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind

data class SelectiveImportState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backupContent: SelectableDatabaseContent? = null,
    val sourceSnapshotBundle: SnapshotBundle? = null,
    val sourceMode: WorkspaceImportSourceMode? = null,
    val sourceFormat: WorkspaceSnapshotFormat? = null,
    val previewModel: WorkspaceImportPreviewModel = WorkspaceImportPreviewModel(),
    val previewSummary: WorkspaceImportPreviewSummary = WorkspaceImportPreviewSummary(),
    val selection: WorkspaceSelectiveImportSelection = WorkspaceSelectiveImportSelection(),
)

data class SelectableDatabaseContent(
    val projects: List<SelectableDiffItem<Context>> = emptyList(),
    val goals: List<SelectableDiffItem<Goal>> = emptyList(),
    val workspaceBacklogEntries: List<SelectableDiffItem<CanonicalBacklogPreviewRow>> = emptyList(),
    val legacyNotes: List<SelectableDiffItem<LegacyNoteEntity>> = emptyList(),
    val activityRecords: List<SelectableDiffItem<ActivityRecord>> = emptyList(),
    val documents: List<SelectableDiffItem<NoteDocumentEntity>> = emptyList(),
    val checklists: List<SelectableDiffItem<ChecklistEntity>> = emptyList(),
    val checklistItems: List<SelectableDiffItem<ChecklistItemEntity>> = emptyList(), // Dependent, not directly selectable
    val linkItems: List<SelectableDiffItem<LinkItemEntity>> = emptyList(),
    val inboxRecords: List<SelectableDiffItem<InboxRecord>> = emptyList(),
    val contextLogs: List<SelectableDiffItem<ContextLog>> = emptyList(),
    val scripts: List<SelectableDiffItem<ScriptEntity>> = emptyList(),
    val attachments: List<SelectableDiffItem<AttachmentEntity>> = emptyList(),
    val allContextAttachmentCrossRefs: List<ContextAttachmentCrossRef> = emptyList(), // Dependent, not directly selectable
)

data class CanonicalBacklogPreviewRow(
    val entry: WorkspaceBacklogEntrySnapshot,
    val title: String,
    val subtitle: String,
)

data class SelectableDiffItem<T>(
    val item: T,
    val status: DiffStatus,
    val isSelected: Boolean = false,
    val isSelectable: Boolean = true,
    val changeInfo: String? = null,
)

fun BackupDiff.toSelectable(source: SnapshotBundle): SelectableDatabaseContent {
    fun <T, R> mapDiff(
        diff: DiffResult<T>,
        toEntity: (T) -> R,
        updatedInfo: (UpdatedItem<T>) -> String? = { null },
    ): List<SelectableDiffItem<R>> {
        val newItems =
            diff.added.map {
                SelectableDiffItem(item = toEntity(it), status = DiffStatus.NEW, isSelected = true, isSelectable = true)
            }
        val updatedItems =
            diff.updated.map {
                SelectableDiffItem(
                    item = toEntity(it.incoming),
                    status = DiffStatus.UPDATED,
                    isSelected = true,
                    isSelectable = true,
                    changeInfo = updatedInfo(it),
                )
            }
        val deletedItems =
            diff.deleted.map {
                SelectableDiffItem(item = toEntity(it), status = DiffStatus.DELETED, isSelected = false, isSelectable = false)
            }
        return newItems + updatedItems + deletedItems
    }

    return SelectableDatabaseContent(
        projects = mapDiff(this.projects, { it.toEntity() }),
        goals = mapDiff(this.goals, { it.toEntity() }),
        workspaceBacklogEntries = source.toSelectableCanonicalBacklog(),
        activityRecords = mapDiff(this.activityRecords, { it.toEntity() }),
        documents = mapDiff(this.documents, { it.toEntity() }),
        checklists = mapDiff(this.checklists, { it.toEntity() }),
        checklistItems = mapDiff(this.checklistItems, { it.toEntity() }),
        linkItems = mapDiff(this.linkItems, { it.toEntity() }),
        inboxRecords = mapDiff(this.inboxRecords, { it.toEntity() }),
        contextLogs = mapDiff(this.contextLogs, { it.toEntity() }),
        scripts = mapDiff(this.scripts, { it.toEntity() }),
        attachments = mapDiff(this.attachments, { it.toEntity() }),
        allContextAttachmentCrossRefs =
            this.contextAttachmentCrossRefs.added.map {
                it.toEntity()
            } + this.contextAttachmentCrossRefs.updated.map { it.incoming.toEntity() },
    )
}

internal fun SnapshotBundle.toSelectableCanonicalBacklog(): List<SelectableDiffItem<CanonicalBacklogPreviewRow>> {
    val entries = workspaceBacklogEntries ?: return emptyList()
    val workspacesById = workspaces.orEmpty().associateBy { it.id }
    val capabilitiesById = workspaceCapabilityInstances.orEmpty().associateBy { it.id }
    val contextsById = contexts.associateBy { it.id }

    return entries.map { entry ->
        val workspace = workspacesById[entry.workspaceId]
        val capability = capabilitiesById[entry.capabilityInstanceId]
        val ownerViolation =
            when {
                workspace == null -> "Missing owner Workspace ${entry.workspaceId}"
                capability == null -> "Missing BACKLOG capability ${entry.capabilityInstanceId}"
                capability.workspaceId != entry.workspaceId -> "BACKLOG capability belongs to another Workspace"
                capability.capabilityType != WorkspaceCapabilityType.BACKLOG.name -> "Capability is not BACKLOG"
                !entry.isDeleted && workspace.isDeleted -> "Live placement belongs to a deleted Workspace"
                else -> null
            }
        val workspaceTitle =
            workspace?.nameOverride?.takeIf { it.isNotBlank() }
                ?: workspace?.sourceContextId?.let { contextsById[it]?.name }
                ?: entry.workspaceId
        val targetKind = runCatching { WorkspaceBacklogTargetKind.valueOf(entry.targetKind) }.getOrNull()
        val targetTitle = resolveCanonicalBacklogTargetTitle(entry, targetKind)

        SelectableDiffItem(
            item =
                CanonicalBacklogPreviewRow(
                    entry = entry,
                    title = targetTitle,
                    subtitle = "$workspaceTitle · ${entry.targetKind}",
                ),
            status = if (entry.isDeleted) DiffStatus.DELETED else DiffStatus.NEW,
            isSelected = ownerViolation == null,
            isSelectable = ownerViolation == null,
            changeInfo = ownerViolation,
        )
    }
}

private fun SnapshotBundle.resolveCanonicalBacklogTargetTitle(
    entry: WorkspaceBacklogEntrySnapshot,
    kind: WorkspaceBacklogTargetKind?,
): String =
    when (kind) {
        WorkspaceBacklogTargetKind.ORIENTATION ->
            managedSubjects.orEmpty().firstOrNull { it.id == entry.targetId }?.title
        WorkspaceBacklogTargetKind.WORKSPACE ->
            workspaces.orEmpty().firstOrNull { it.id == entry.targetId }?.let { workspace ->
                workspace.nameOverride
                    ?: workspace.sourceContextId?.let { contextId -> contexts.firstOrNull { it.id == contextId }?.name }
            }
        WorkspaceBacklogTargetKind.LINK_ITEM ->
            linkItemEntities.firstOrNull { it.id == entry.targetId }?.linkData?.let { it.displayName ?: it.target }
        WorkspaceBacklogTargetKind.LEGACY_NOTE -> notes.firstOrNull { it.id == entry.targetId }?.title
        WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
        -> documents.firstOrNull { it.id == entry.targetId }?.name
        WorkspaceBacklogTargetKind.CHECKLIST -> checklists.firstOrNull { it.id == entry.targetId }?.name
        WorkspaceBacklogTargetKind.MUSIC_NOTE -> musicNotes.firstOrNull { it.id == entry.targetId }?.name
        null -> null
    }?.takeIf { it.isNotBlank() } ?: "${entry.targetKind} · ${entry.targetId}"
