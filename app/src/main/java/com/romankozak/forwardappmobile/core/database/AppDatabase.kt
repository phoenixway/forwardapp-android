package com.romankozak.forwardappmobile.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao

import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.core.database.models.*

@Database(
    entities = [
        ConversationEntity::class,
        Goal::class,
        ProjectEntity::class,
        ActivityRecord::class,
        InboxRecord::class,
        ChatMessageEntity::class,
        ProjectExecutionLog::class,

        DayTask::class,
        DailyMetric::class,
        LegacyNoteRoomEntity::class,
        NoteDocumentRoomEntity::class,
        NoteDocumentItemRoomEntity::class,
        RecentItemRoomEntity::class,
        ConversationFolderEntity::class,
        RecurringTask::class,

        GoalFts::class,
        ProjectFts::class,
        ActivityRecordFts::class,
        LegacyNoteFts::class,
        RecurringTaskFts::class,
    ],
    version = 68,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 67, to = 68)
    ]
)
@TypeConverters(Converters::class, DailyPlanConverters::class, ProjectTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao

    abstract fun activityRecordDao(): ActivityRecordDao

    abstract fun inboxRecordDao(): InboxRecordDao

    abstract fun chatDao(): ChatDao



    abstract fun conversationFolderDao(): ConversationFolderDao

    abstract fun projectManagementDao(): ProjectManagementDao



    abstract fun dayTaskDao(): DayTaskDao



    abstract fun dailyMetricDao(): DailyMetricDao

    abstract fun recurringTaskDao(): RecurringTaskDao
}