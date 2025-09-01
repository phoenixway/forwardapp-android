package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.*

/**
 * Головний контейнер для повної резервної копії всього додатку.
 * @param backupSchemaVersion Версія схеми самого файлу бекапу. Дозволить у майбутньому робити міграції.
 * @param exportedAt Час створення резервної копії.
 * @param database Вміст бази даних Room.
 * @param settings Вміст сховища налаштувань DataStore.
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
    val notes: List<Note>,
    val listItems: List<ListItem>,
    val activityRecords: List<ActivityRecord>,
    val recentListEntries: List<RecentListEntry>,
    val linkItemEntities: List<LinkItemEntity>
)

/**
 * Контейнер для всіх налаштувань з DataStore.
 * Зберігаємо все у вигляді простої мапи String-to-String для універсальності.
 */
data class SettingsContent(
    val settings: Map<String, String>
)