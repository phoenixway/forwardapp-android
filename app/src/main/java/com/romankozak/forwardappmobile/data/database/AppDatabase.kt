package com.romankozak.forwardappmobile.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.database.models.DailyPlanConverters

@Database(
    entities = [
        Goal::class,
        Project::class,
        ListItem::class,
        ActivityRecord::class,
        ActivityRecordFts::class,
        RecentProjectEntry::class,
        LinkItemEntity::class,
        InboxRecord::class,
        ChatMessageEntity::class,
        ProjectExecutionLog::class,
        DayPlan::class,
        DayTask::class,
        DailyMetric::class
    ],
    version = 32,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
    ],
    exportSchema = true,
)
@TypeConverters(Converters::class, DailyPlanConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun goalDao(): GoalDao
    abstract fun listItemDao(): ListItemDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun recentProjectDao(): RecentProjectDao
    abstract fun linkItemDao(): LinkItemDao
    abstract fun inboxRecordDao(): InboxRecordDao
    abstract fun chatDao(): ChatDao
    abstract fun projectManagementDao(): ProjectManagementDao
    abstract fun dayPlanDao(): DayPlanDao
    abstract fun dayTaskDao(): DayTaskDao
    abstract fun dailyMetricDao(): DailyMetricDao
}