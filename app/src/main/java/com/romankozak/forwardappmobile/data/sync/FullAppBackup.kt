package com.romankozak.forwardappmobile.data.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.data.database.models.*

data class FullAppBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val database: DatabaseContent,
    val settings: SettingsContent,
)

data class DatabaseContent(
    val goals: List<Goal>,
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<Project>,
    val listItems: List<ListItem>,
    val notes: List<NoteEntity>?,
    val customLists: List<CustomListEntity>?,
    val customListItems: List<CustomListItemEntity>?,
    val activityRecords: List<ActivityRecord>? = null,
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry>? = null,
    val linkItemEntities: List<LinkItemEntity>? = null,
    val inboxRecords: List<InboxRecord>? = null,
    val projectExecutionLogs: List<ProjectExecutionLog>? = null,
)

data class SettingsContent(
    val settings: Map<String, String>,
)

data class RecentProjectEntry(
    val projectId: String,
    val timestamp: Long
)
