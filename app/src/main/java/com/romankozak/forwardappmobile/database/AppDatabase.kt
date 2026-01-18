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
import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord
import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecordFts
import com.romankozak.forwardappmobile.features.ai.data.models.AiEventEntity
import com.romankozak.forwardappmobile.features.ai.data.models.ChatMessageEntity
import com.romankozak.forwardappmobile.features.ai.data.models.ConversationEntity
import com.romankozak.forwardappmobile.features.ai.data.models.ConversationFolderEntity
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DailyMetric
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DailyPlanConverters
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayPlan
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayTask
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteFts
import com.romankozak.forwardappmobile.features.lifestate.data.models.LifeSystemStateEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectArtifact
import com.romankozak.forwardappmobile.features.recent.data.models.RecentItem
import com.romankozak.forwardappmobile.features.daymanagement.data.models.RecurringTask
import com.romankozak.forwardappmobile.features.daymanagement.data.models.RecurringTaskFts
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder
import com.romankozak.forwardappmobile.features.attachments.data.models.ScriptEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.SystemAppEntity

import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.ai.data.models.AiInsightEntity
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ProjectAttachmentCrossRef
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectArtifactDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.Converters
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.contexts.data.models.GoalFts
import com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextsFts
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextConfiguration
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStructureItem
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextTypeConverter
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedGroupConverter
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfile
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfileItem
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMissionAttachmentCrossRef

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
        ProjectStructureItem::class,
        RecentItem::class,
        ConversationFolderEntity::class,
        RecurringTask::class,
        Reminder::class,
        ProjectArtifact::class,
        ProjectAttachmentCrossRef::class,
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
    version = 93,
    exportSchema = true,
)
@TypeConverters(Converters::class, DailyPlanConverters::class, ContextTypeConverter::class, ReservedGroupConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun goalDao(): GoalDao
    abstract fun listItemDao(): ListItemDao
    abstract fun backlogOrderDao(): BacklogOrderDao
    abstract fun linkItemDao(): LinkItemDao
    abstract fun inboxRecordDao(): InboxRecordDao
    abstract fun projectManagementDao(): ProjectManagementDao
    abstract fun noteDocumentDao(): NoteDocumentDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun structurePresetDao(): StructurePresetDao
    abstract fun structurePresetItemDao(): StructurePresetItemDao
    abstract fun projectStructureDao(): ProjectStructureDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun chatDao(): ChatDao
    abstract fun conversationFolderDao(): ConversationFolderDao
    abstract fun dailyMetricDao(): DailyMetricDao
    abstract fun dayPlanDao(): DayPlanDao
    abstract fun dayTaskDao(): DayTaskDao
    abstract fun legacyNoteDao(): LegacyNoteDao
    abstract fun projectArtifactDao(): ProjectArtifactDao
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
