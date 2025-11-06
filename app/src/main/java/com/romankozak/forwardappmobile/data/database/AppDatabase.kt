package com.romankozak.forwardappmobile.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentRoomEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRefRoom

@Database(
    entities = [
        ConversationEntity::class,
        Goal::class,
        ProjectEntity::class,
        ListItem::class,
        ActivityRecord::class,
        LinkItemEntity::class,
        AttachmentRoomEntity::class,
        InboxRecord::class,
        ChatMessageEntity::class,
        ProjectExecutionLog::class,
        DayPlan::class,
        DayTask::class,
        DailyMetric::class,
        LegacyNoteEntity::class,
        NoteDocumentEntity::class,
        NoteDocumentItemEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        RecentItem::class,
        ConversationFolderEntity::class,
        RecurringTask::class,
        Reminder::class,
        ProjectAttachmentCrossRefRoom::class,

        GoalFts::class,
        ProjectFts::class,
        ActivityRecordFts::class,
        LegacyNoteFts::class,
        RecurringTaskFts::class,
    ],
    version = 64,
    exportSchema = true,
)
@TypeConverters(Converters::class, DailyPlanConverters::class, ProjectTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun goalDao(): GoalDao

    abstract fun listItemDao(): ListItemDao

    abstract fun activityRecordDao(): ActivityRecordDao

    abstract fun linkItemDao(): LinkItemDao

    abstract fun inboxRecordDao(): InboxRecordDao

    abstract fun chatDao(): ChatDao

    abstract fun attachmentDao(): AttachmentDao

    abstract fun conversationFolderDao(): ConversationFolderDao

    abstract fun projectManagementDao(): ProjectManagementDao

    abstract fun dayPlanDao(): DayPlanDao

    abstract fun dayTaskDao(): DayTaskDao



    abstract fun dailyMetricDao(): DailyMetricDao

    abstract fun legacyNoteDao(): LegacyNoteDao

    abstract fun noteDocumentDao(): NoteDocumentDao

    abstract fun checklistDao(): ChecklistDao

    abstract fun recentItemDao(): RecentItemDao

    abstract fun recurringTaskDao(): RecurringTaskDao

    abstract fun reminderDao(): ReminderDao
}
