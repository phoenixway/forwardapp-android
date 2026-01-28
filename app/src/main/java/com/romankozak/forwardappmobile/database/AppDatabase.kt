package com.romankozak.forwardappmobile.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.LifeSystemStateDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.core.data.models.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.ActivityRecordFts
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.core.data.models.ai.AiEventEntity
import com.romankozak.forwardappmobile.core.data.models.ai.AiInsightEntity
import com.romankozak.forwardappmobile.core.data.models.ai.ChatMessageEntity
import com.romankozak.forwardappmobile.core.data.models.ai.ConversationEntity
import com.romankozak.forwardappmobile.core.data.models.ai.ConversationFolderEntity
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.LegacyNoteFts
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.core.data.models.ScriptEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextArtifactDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.ContextLog
import com.romankozak.forwardappmobile.core.data.models.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.ContextsFts
import com.romankozak.forwardappmobile.core.data.models.Converters
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.GoalFts
import com.romankozak.forwardappmobile.core.data.models.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.day_management.DailyPlanConverters
import com.romankozak.forwardappmobile.core.data.models.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.day_management.RecurringTaskFts
import com.romankozak.forwardappmobile.core.data.models.LifeSystemStateEntity
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.core.data.models.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.tactical.TacticalMissionAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.RecentItem
import com.romankozak.forwardappmobile.core.data.models.Reminder

@Database(
    entities = [
        ConversationEntity::class,
        Goal::class,
        Context::class,
        BacklogItem::class,
        BacklogOrder::class,
        ActivityRecord::class,
        LinkItemEntity::class,
        AttachmentEntity::class,
        InboxRecord::class,
        ChatMessageEntity::class,
        ContextLog::class,
        DayPlan::class,
        DayTask::class,
        DailyMetric::class,
        LegacyNoteEntity::class,
        NoteDocumentEntity::class,
        NoteDocumentItemEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        ScriptEntity::class,
        ContextRoleProfile::class,
        ContextRoleProfileItem::class,
        ContextConfiguration::class,
        ContextStructureItem::class,
        RecentItem::class,
        ConversationFolderEntity::class,
        RecurringTask::class,
        Reminder::class,
        ContextArtifact::class,
        ContextAttachmentCrossRef::class,
        SystemAppEntity::class,
        TacticalMission::class,
        TacticalMissionAttachmentCrossRef::class,
        AiEventEntity::class,
        LifeSystemStateEntity::class,
        AiInsightEntity::class,
        GoalFts::class,
        ContextsFts::class,
        ActivityRecordFts::class,
        LegacyNoteFts::class,
        RecurringTaskFts::class,
    ],
    version = 95,
    exportSchema = true,
)
@TypeConverters(Converters::class, DailyPlanConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contextDao(): ContextDao

    abstract fun goalDao(): GoalDao

    abstract fun listItemDao(): ListItemDao

    abstract fun backlogOrderDao(): BacklogOrderDao

    abstract fun linkItemDao(): LinkItemDao

    abstract fun inboxRecordDao(): InboxRecordDao

    abstract fun contextManagementDao(): ContextManagementDao

    abstract fun noteDocumentDao(): NoteDocumentDao

    abstract fun checklistDao(): ChecklistDao

    abstract fun structurePresetDao(): StructurePresetDao

    abstract fun structurePresetItemDao(): StructurePresetItemDao

    abstract fun contextStructureDao(): ContextStructureDao

    abstract fun activityRecordDao(): ActivityRecordDao

    abstract fun chatDao(): ChatDao

    abstract fun conversationFolderDao(): ConversationFolderDao

    abstract fun dailyMetricDao(): DailyMetricDao

    abstract fun dayPlanDao(): DayPlanDao

    abstract fun dayTaskDao(): DayTaskDao

    abstract fun legacyNoteDao(): LegacyNoteDao

    abstract fun contextArtifactDao(): ContextArtifactDao

    abstract fun recentItemDao(): RecentItemDao

    abstract fun recurringTaskDao(): RecurringTaskDao

    abstract fun reminderDao(): ReminderDao

    abstract fun scriptDao(): ScriptDao

    abstract fun systemAppDao(): SystemAppDao

    abstract fun aiEventDao(): AiEventDao

    abstract fun lifeSystemStateDao(): LifeSystemStateDao

    abstract fun tacticalMissionDao(): TacticalMissionDao

    abstract fun attachmentDao(): AttachmentDao

    abstract fun aiInsightDao(): AiInsightDao
}
