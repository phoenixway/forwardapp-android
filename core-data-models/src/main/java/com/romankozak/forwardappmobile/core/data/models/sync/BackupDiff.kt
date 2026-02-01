package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.activity.ActivityRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.DailyMetricSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.RecurringTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.misc.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.reminders.ReminderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.TacticalMissionAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai.AiEventSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai.AiInsightSnapshot

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
    val documentItems: DiffResult<NoteDocumentItemSnapshot> = DiffResult(),
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
    val dailyMetrics: DiffResult<DailyMetricSnapshot> = DiffResult(),
    val reminders: DiffResult<ReminderSnapshot> = DiffResult(),
    val recurringTasks: DiffResult<RecurringTaskSnapshot> = DiffResult(),
    val tacticalMissions: DiffResult<TacticalMissionSnapshot> = DiffResult(),
    val tacticalMissionAttachments: DiffResult<TacticalMissionAttachmentCrossRefSnapshot> = DiffResult(),
    val aiEvents: DiffResult<AiEventSnapshot> = DiffResult(),
    val aiInsights: DiffResult<AiInsightSnapshot> = DiffResult(),
    val lifeSystemStates: DiffResult<LifeSystemStateSnapshot> = DiffResult(),
    val contextRoleProfiles: DiffResult<ContextRoleProfileSnapshot> = DiffResult(),
    val contextRoleProfileItems: DiffResult<ContextRoleProfileItemSnapshot> = DiffResult(),
    val contextConfigurations: DiffResult<ContextConfigurationSnapshot> = DiffResult(),
    val projectStructureItems: DiffResult<ContextStructureItemSnapshot> = DiffResult(),
)
