package com.romankozak.forwardappmobile.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*

@Database(
    entities = [
        ConversationEntity::class,
        Goal::class,
        Project::class,
        ListItem::class,
        ActivityRecord::class,
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
        RecentItem::class,
        ConversationFolderEntity::class,
        RecurringTask::class,
        ReminderInfo::class,

        GoalFts::class,
        ProjectFts::class,
        ActivityRecordFts::class,
        NoteFts::class,
        RecurringTaskFts::class,
        ProjectReminderInfo::class,
    ],
    version = 49,
    exportSchema = true,
)
@TypeConverters(Converters::class, DailyPlanConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun goalDao(): GoalDao

    abstract fun listItemDao(): ListItemDao

    abstract fun activityRecordDao(): ActivityRecordDao

    abstract fun linkItemDao(): LinkItemDao

    abstract fun inboxRecordDao(): InboxRecordDao

    abstract fun chatDao(): ChatDao

    abstract fun conversationFolderDao(): ConversationFolderDao

    abstract fun projectManagementDao(): ProjectManagementDao

    abstract fun dayPlanDao(): DayPlanDao

    abstract fun dayTaskDao(): DayTaskDao

    abstract fun dailyMetricDao(): DailyMetricDao

    abstract fun noteDao(): NoteDao

    abstract fun customListDao(): CustomListDao

    abstract fun recentItemDao(): RecentItemDao

    abstract fun recurringTaskDao(): RecurringTaskDao

    abstract fun reminderInfoDao(): ReminderInfoDao

    abstract fun projectReminderInfoDao(): ProjectReminderInfoDao
}
