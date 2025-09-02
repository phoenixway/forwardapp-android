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
    // Основні таблиці, які були з самого початку
    val goals: List<Goal>,
    val goalLists: List<GoalList>,
    val listItems: List<ListItem>,


    // --- ПОЧАТОК ЗМІНИ: Робимо нові таблиці необов'язковими ---
    // Це забезпечить сумісність зі старими бекапами, де цих таблиць не було.
    val activityRecords: List<ActivityRecord>? = null,
    val recentListEntries: List<RecentListEntry>? = null,
    val linkItemEntities: List<LinkItemEntity>? = null,
    val inboxRecords: List<InboxRecord>? = null
    // --- КІНЕЦЬ ЗМІНИ ---

)

/**
 * Контейнер для всіх налаштувань з DataStore.
 */
data class SettingsContent(
    val settings: Map<String, String>
)