package com.romankozak.forwardappmobile.database

import androidx.room.AutoMigration
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
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.ActivityRecordFts
import com.romankozak.forwardappmobile.data.database.models.AiEventEntity
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationFolderEntity
import com.romankozak.forwardappmobile.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteFts
import com.romankozak.forwardappmobile.data.database.models.LifeSystemStateEntity
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.data.database.models.RecurringTask
import com.romankozak.forwardappmobile.data.database.models.RecurringTaskFts
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.data.database.models.SystemAppEntity
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.ai.data.models.AiInsightEntity
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
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
import com.romankozak.forwardappmobile.features.contexts.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.Converters
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.contexts.data.models.GoalFts
import com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItem
import com.romankozak.forwardappmobile.features.contexts.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectFts
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStructure
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStructureItem
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectTypeConverter
import com.romankozak.forwardappmobile.features.contexts.data.models.StructurePreset
import com.romankozak.forwardappmobile.features.contexts.data.models.StructurePresetItem
import com.romankozak.forwardappmobile.features.daymanagement.data.database.DailyPlanConverters
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMissionAttachmentCrossRef

@Database(
    entities = [
        ConversationEntity::class,
        Goal::class,
        Project::class,
        ListItem::class,
        BacklogOrder::class,
        ActivityRecord::class,
        LinkItemEntity::class,
        AttachmentEntity::class,
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
        ScriptEntity::class,
        StructurePreset::class,
        StructurePresetItem::class,
        ProjectStructure::class,
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
        ProjectFts::class,
        ActivityRecordFts::class,
        LegacyNoteFts::class,
        RecurringTaskFts::class,
    ],
    version = 93,
    exportSchema = true,
)
@TypeConverters(Converters::class, DailyPlanConverters::class, ProjectTypeConverter::class, ReservedGroupConverter::class)
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
