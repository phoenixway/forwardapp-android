package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.activity.ActivityRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DailyMetricSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurringTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.reminders.ReminderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalActivitySlot
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.AiEventSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.AiInsightSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.AttachmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.NoteDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.MusicNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ScriptSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogOrderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextConfigurationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextStructureItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.GoalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.LinkItemEntitySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.LifeSystemStateSnapshot

enum class DiffStatus { NEW, UPDATED, DELETED }

data class UpdatedItem<T>(val local: T, val incoming: T)

data class DiffResult<T>(
    val added: List<T> = emptyList(),
    val updated: List<UpdatedItem<T>> = emptyList(),
    val deleted: List<T> = emptyList(),
)

data class BackupDiff(
    val projects: DiffResult<ContextSnapshot> = DiffResult(),
    val goals: DiffResult<GoalSnapshot> = DiffResult(),
    val backlogItems: DiffResult<BacklogItemSnapshot> = DiffResult(),
    val backlogOrders: DiffResult<BacklogOrderSnapshot> = DiffResult(),
    val legacyNotes: DiffResult<LegacyNoteSnapshot> = DiffResult(),
    val activityRecords: DiffResult<ActivityRecordSnapshot> = DiffResult(),
    val documents: DiffResult<NoteDocumentSnapshot> = DiffResult(),
    val musicNotes: DiffResult<MusicNoteSnapshot> = DiffResult(),
    val checklists: DiffResult<ChecklistSnapshot> = DiffResult(),
    val checklistItems: DiffResult<ChecklistItemSnapshot> = DiffResult(),
    val linkItems: DiffResult<LinkItemEntitySnapshot> = DiffResult(),
    val inboxRecords: DiffResult<InboxRecordSnapshot> = DiffResult(),
    val contextLogs: DiffResult<ContextLogSnapshot> = DiffResult(),
    val scripts: DiffResult<ScriptSnapshot> = DiffResult(),
    val attachments: DiffResult<AttachmentSnapshot> = DiffResult(),
    val contextAttachmentCrossRefs: DiffResult<ContextAttachmentCrossRefSnapshot> = DiffResult(),
    val dayPlans: DiffResult<DayPlanSnapshot> = DiffResult(),
    val dayTasks: DiffResult<DayTaskSnapshot> = DiffResult(),
    val dayThemeDocuments: DiffResult<DayThemeDocumentSnapshot> = DiffResult(),
    val dailyMetrics: DiffResult<DailyMetricSnapshot> = DiffResult(),
    val reminders: DiffResult<ReminderSnapshot> = DiffResult(),
    val recurringTasks: DiffResult<RecurringTaskSnapshot> = DiffResult(),
    val tacticalMissions: DiffResult<TacticalMissionSnapshot> = DiffResult(),
    val tacticalMissionAttachments: DiffResult<TacticalMissionAttachmentCrossRefSnapshot> = DiffResult(),
    val tacticalIterations: DiffResult<TacticalIteration> = DiffResult(),
    val missionStreams: DiffResult<MissionStream> = DiffResult(),
    val tacticalActivitySlots: DiffResult<TacticalActivitySlot> = DiffResult(),
    val arcQuests: DiffResult<ArcQuestEntity> = DiffResult(),
    val aiEvents: DiffResult<AiEventSnapshot> = DiffResult(),
    val aiInsights: DiffResult<AiInsightSnapshot> = DiffResult(),
    val lifeSystemStates: DiffResult<LifeSystemStateSnapshot> = DiffResult(),
    val contextRoleProfiles: DiffResult<ContextRoleProfileSnapshot> = DiffResult(),
    val contextRoleProfileItems: DiffResult<ContextRoleProfileItemSnapshot> = DiffResult(),
    val contextConfigurations: DiffResult<ContextConfigurationSnapshot> = DiffResult(),
    val projectStructureItems: DiffResult<ContextStructureItemSnapshot> = DiffResult(),
)
