// com/romankozak/forwardappmobile/data/database/models/BackupModels.kt

package com.romankozak.forwardappmobile.data.database.models

import com.google.gson.annotations.SerializedName

// Модель для парсингу кореневого об'єкта JSON
data class FullAppBackup(
    @SerializedName("database")
    val database: DatabaseContent,
    // Можете додати інші секції бекапу, якщо вони є
)

// Модель для парсингу об'єкта "database"
data class DatabaseContent(
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<Project>, // <--- ОСНОВНА ЗМІНА 1

    val goals: List<Goal>,
    val listItems: List<ListItem>,
    val activityRecords: List<ActivityRecord>?,
    val inboxRecords: List<InboxRecord>?,

    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry>?, // <--- ОСНОВНА ЗМІНА 2

    val linkItemEntities: List<LinkItemEntity>?,
    val projectExecutionLogs: List<ProjectExecutionLog>?
)