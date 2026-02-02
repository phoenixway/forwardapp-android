package com.romankozak.forwardappmobile.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecordFts
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextsFts
import com.romankozak.forwardappmobile.core.data.models.entities.Converters
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalFts
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteFts
import com.romankozak.forwardappmobile.core.data.models.entities.LifeSystemStateEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiEventEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiInsightEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ChatMessageEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationFolderEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyPlanConverters
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTaskFts
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMissionAttachmentCrossRef
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
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
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
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao

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
    version = 100,
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
