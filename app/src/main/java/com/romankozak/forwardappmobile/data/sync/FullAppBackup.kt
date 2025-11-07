package com.romankozak.forwardappmobile.data.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.database.models.*

data class FullAppBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val database: DatabaseContent,
    val settings: SettingsContent? = null,
)

data class DatabaseContent(
    val goals: List<Goal>,
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<ProjectEntity>,
    val listItems: List<ListItem>,
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<LegacyNoteEntity>? = null,
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<NoteDocumentEntity>? = null,
    @SerializedName(value = "documentItems", alternate = ["customListItems"])
    val documentItems: List<NoteDocumentItemEntity>? = null,
    val checklists: List<ChecklistEntity>? = null,
    val checklistItems: List<ChecklistItemEntity>? = null,
    val activityRecords: List<ActivityRecord>? = null,
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry>? = null,

    val inboxRecords: List<InboxRecord>? = null,
    val projectExecutionLogs: List<ProjectExecutionLog>? = null,
)

data class SettingsContent(
    val settings: Map<String, String>,
)
