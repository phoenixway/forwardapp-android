package com.romankozak.forwardappmobile.data.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.data.database.models.*

data class FullAppBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val database: DatabaseContent,
    val settings: SettingsContent? = null,
)

data class DatabaseContent(
    val goals: List<Goal> = emptyList(),
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<Project> = emptyList(),
    val listItems: List<ListItem> = emptyList(),
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<LegacyNoteEntity> = emptyList(),
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<NoteDocumentEntity> = emptyList(),
    @SerializedName(value = "documentItems", alternate = ["customListItems"])
    val documentItems: List<NoteDocumentItemEntity> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val activityRecords: List<ActivityRecord> = emptyList(),
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry> = emptyList(),
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
    val inboxRecords: List<InboxRecord> = emptyList(),
    val projectExecutionLogs: List<ProjectExecutionLog> = emptyList(),
    @SerializedName(value = "attachments", alternate = ["attachment_items"])
    val attachments: List<com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity> = emptyList(),
    @SerializedName(value = "projectAttachmentCrossRefs", alternate = ["project_attachment_links"])
    val projectAttachmentCrossRefs: List<com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef> = emptyList(),
)

data class SettingsContent(
    val settings: Map<String, String>,
)

data class RecentProjectEntry(
    val projectId: String,
    val timestamp: Long
)
