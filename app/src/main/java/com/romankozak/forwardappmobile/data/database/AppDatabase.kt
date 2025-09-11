package com.romankozak.forwardappmobile.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*

@Database(
    entities = [
        Goal::class,
        GoalList::class,
        ListItem::class,
        ActivityRecord::class,
        ActivityRecordFts::class,
        RecentListEntry::class,
        LinkItemEntity::class,
        InboxRecord::class,
        ChatMessageEntity::class,
        ProjectExecutionLog::class,
    ],
    version = 28, // <-- ЗМІНЕНО
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalListDao(): GoalListDao
    abstract fun goalDao(): GoalDao
    abstract fun listItemDao(): ListItemDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun recentListDao(): RecentListDao
    abstract fun linkItemDao(): LinkItemDao
    abstract fun inboxRecordDao(): InboxRecordDao
    abstract fun chatDao(): ChatDao
    abstract fun projectManagementDao(): ProjectManagementDao
}

