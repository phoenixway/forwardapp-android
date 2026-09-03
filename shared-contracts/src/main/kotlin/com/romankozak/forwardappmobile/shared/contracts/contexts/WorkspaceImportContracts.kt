package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WorkspaceSnapshotFormat(
    val title: String,
) {
    Desktop("Desktop Snapshot"),
    AndroidSnapshotBundleV2("Android Snapshot Bundle V2"),
    AndroidLegacyDatabase("Android Legacy Backup"),
}

@Serializable
data class ResolvedWorkspaceSnapshot(
    val snapshot: DesktopWorkspaceSnapshot,
    val format: WorkspaceSnapshotFormat,
)

@Serializable
enum class WorkspaceImportSourceMode(
    val title: String,
) {
    LegacyDatabase("Legacy Database"),
    SnapshotBundle("Snapshot Bundle"),
    DesktopSnapshot("Desktop Snapshot"),
}

@Serializable
data class WorkspaceImportDescriptor(
    val format: WorkspaceSnapshotFormat,
    val sourceMode: WorkspaceImportSourceMode,
)

@Serializable
data class WorkspaceSelectiveImportSelection(
    val selectedContextIds: Set<String> = emptySet(),
    val selectedGoalIds: Set<String> = emptySet(),
    val selectedWorkspaceBacklogEntryIds: Set<String> = emptySet(),
    val selectedDocumentIds: Set<String> = emptySet(),
    val selectedChecklistIds: Set<String> = emptySet(),
    val selectedLinkItemIds: Set<String> = emptySet(),
    val selectedInboxRecordIds: Set<String> = emptySet(),
    val selectedContextLogIds: Set<String> = emptySet(),
    val selectedScriptIds: Set<String> = emptySet(),
    val selectedAttachmentIds: Set<String> = emptySet(),
    val selectedActivityRecordIds: Set<String> = emptySet(),
)

@Serializable
enum class WorkspaceImportPreviewSectionKind(
    val title: String,
) {
    Contexts("Contexts"),
    Goals("Goals"),
    Backlog("Backlog"),
    LegacyNotes("Legacy Notes"),
    ActivityRecords("Activity Records"),
    Documents("Documents"),
    Checklists("Checklists"),
    LinkItems("Link Items"),
    InboxRecords("Inbox Records"),
    ContextLogs("Context Logs"),
    Scripts("Scripts"),
    Attachments("Attachments"),
}

@Serializable
data class WorkspaceImportPreviewSectionSummary(
    val kind: WorkspaceImportPreviewSectionKind,
    val totalCount: Int,
    val selectedCount: Int,
    val newCount: Int,
    val updatedCount: Int,
    val deletedCount: Int,
)

@Serializable
data class WorkspaceImportPreviewItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val status: WorkspaceImportPreviewItemStatus,
    val isSelected: Boolean,
    val isSelectable: Boolean,
)

@Serializable
enum class WorkspaceImportPreviewItemStatus {
    New,
    Updated,
    Deleted,
}

@Serializable
data class WorkspaceImportPreviewSection(
    val kind: WorkspaceImportPreviewSectionKind,
    val title: String,
    val items: List<WorkspaceImportPreviewItem> = emptyList(),
)

@Serializable
data class WorkspaceImportPreviewSummary(
    val sections: List<WorkspaceImportPreviewSectionSummary> = emptyList(),
) {
    val totalCount: Int get() = sections.sumOf { it.totalCount }
    val totalSelectedCount: Int get() = sections.sumOf { it.selectedCount }
    val totalNewCount: Int get() = sections.sumOf { it.newCount }
    val totalUpdatedCount: Int get() = sections.sumOf { it.updatedCount }
    val totalDeletedCount: Int get() = sections.sumOf { it.deletedCount }
}

@Serializable
data class WorkspaceImportPreviewModel(
    val sections: List<WorkspaceImportPreviewSection> = emptyList(),
)
