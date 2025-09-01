package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.*

/**
 * Головний контейнер для повної резервної копії всього додатку.
 */
data class FullAppBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val database: DatabaseContent,
    val settings: SettingsContent
)

/**
 * Контейнер для всіх таблиць з бази даних Room.
 */
data class DatabaseContent(
    val goals: List<Goal>,
    val goalLists: List<GoalList>,
    val listItems: List<ListItem>,
    val activityRecords: List<ActivityRecord>,
    val recentListEntries: List<RecentListEntry>,
    val linkItemEntities: List<LinkItemEntity>,
    val inboxRecords: List<InboxRecord> // <-- ДОДАНО
)

/**
 * Контейнер для всіх налаштувань з DataStore.
 */
data class SettingsContent(
    val settings: Map<String, String>
)