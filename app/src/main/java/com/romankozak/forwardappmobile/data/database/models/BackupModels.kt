package com.romankozak.forwardappmobile.data.database.models

import com.google.gson.annotations.SerializedName

data class RecentProjectEntry(
    val projectId: String,
    val timestamp: Long
)

data class FullAppBackup(
    @SerializedName("database")
    val database: DatabaseContent,
)

data class DatabaseContent(
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<ProjectEntity>,
    val goals: List<Goal>,
    val listItems: List<ListItem>,
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<LegacyNoteEntity>? = emptyList(),
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<NoteDocumentEntity>? = emptyList(),
    @SerializedName(value = "documentItems", alternate = ["customListItems"])
    val documentItems: List<NoteDocumentItemEntity>? = emptyList(),
    val checklists: List<ChecklistEntity>? = emptyList(),
    val checklistItems: List<ChecklistItemEntity>? = emptyList(),
    val activityRecords: List<ActivityRecord>? = emptyList(),
    val inboxRecords: List<InboxRecord>? = emptyList(),
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry>? = emptyList(),
    val linkItemEntities: List<LinkItemEntity>? = emptyList(),
    val projectExecutionLogs: List<ProjectExecutionLog>? = emptyList(),
)
