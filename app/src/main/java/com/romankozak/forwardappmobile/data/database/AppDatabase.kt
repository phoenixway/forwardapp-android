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
        Project::class,
        ListItem::class,
        ActivityRecord::class,
        RecentProjectEntry::class,
        LinkItemEntity::class,
        InboxRecord::class,
        ChatMessageEntity::class,
        ProjectExecutionLog::class,
        DayPlan::class,
        DayTask::class,
        DailyMetric::class,
        NoteEntity::class,
        CustomListEntity::class,
        CustomListItemEntity::class,
        
        GoalFts::class,
        ProjectFts::class,
        ActivityRecordFts::class,
        NoteFts::class,
    ],
    version = 35,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 32, to = 33),
        AutoMigration(from = 33, to = 34),
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

    abstract fun noteDao(): NoteDao

    abstract fun customListDao(): CustomListDao
}
