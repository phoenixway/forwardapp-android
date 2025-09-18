package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.*

data class FullAppBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val database: DatabaseContent,
    val settings: SettingsContent,
)

data class DatabaseContent(
    val goals: List<Goal>,
    val goalLists: List<GoalList>,
    val listItems: List<ListItem>,
    val activityRecords: List<ActivityRecord>? = null,
    val recentListEntries: List<RecentProjectEntry>? = null,
    val linkItemEntities: List<LinkItemEntity>? = null,
    val inboxRecords: List<InboxRecord>? = null,
    val projectExecutionLogs: List<ProjectExecutionLog>? = null,
)

data class SettingsContent(
    val settings: Map<String, String>,
)
