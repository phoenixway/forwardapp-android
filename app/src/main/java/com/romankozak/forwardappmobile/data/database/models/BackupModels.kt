

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
    val projects: List<Project>,
    val goals: List<Goal>,
    val listItems: List<ListItem>,
    val notes: List<NoteEntity>?,
    val customLists: List<CustomListEntity>?,
    val customListItems: List<CustomListItemEntity>?,
    val activityRecords: List<ActivityRecord>?,
    val inboxRecords: List<InboxRecord>?,
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry>?,
    val linkItemEntities: List<LinkItemEntity>?,
    val projectExecutionLogs: List<ProjectExecutionLog>?,
)
