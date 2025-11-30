package com.romankozak.forwardappmobile.data.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.data.database.models.*

data class FullAppBackup(
    @SerializedName(value = "backupSchemaVersion", alternate = ["a"])
    val backupSchemaVersion: Int = 1,
    @SerializedName(value = "exportedAt", alternate = ["b"])
    val exportedAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "database", alternate = ["c"])
    val database: DatabaseContent,
    @SerializedName(value = "settings", alternate = ["d"])
    val settings: SettingsContent? = null,
)

data class DatabaseContent(
    @SerializedName(value = "goals", alternate = ["a"])
    val goals: List<Goal> = emptyList(),
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<Project> = emptyList(),
    @SerializedName(value = "listItems", alternate = ["c"])
    val listItems: List<ListItem> = emptyList(),
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<LegacyNoteEntity> = emptyList(),
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<NoteDocumentEntity> = emptyList(),
    @SerializedName(value = "documentItems", alternate = ["customListItems"])
    val documentItems: List<NoteDocumentItemEntity> = emptyList(),
    @SerializedName(value = "checklists", alternate = ["g"])
    val checklists: List<ChecklistEntity> = emptyList(),
    @SerializedName(value = "checklistItems", alternate = ["h"])
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    @SerializedName(value = "activityRecords", alternate = ["i"])
    val activityRecords: List<ActivityRecord> = emptyList(),
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry> = emptyList(),
    @SerializedName(value = "linkItemEntities", alternate = ["k"])
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
    @SerializedName(value = "inboxRecords", alternate = ["l"])
    val inboxRecords: List<InboxRecord> = emptyList(),
    @SerializedName(value = "projectExecutionLogs", alternate = ["m"])
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
