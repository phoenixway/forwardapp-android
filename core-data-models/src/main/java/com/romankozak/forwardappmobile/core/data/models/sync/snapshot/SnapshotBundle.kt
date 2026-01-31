package com.romankozak.forwardappmobile.core.data.models.sync.snapshot

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.activity.ActivityRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.AttachmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.ChecklistItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.ChecklistSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.misc.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.reminders.ReminderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.TacticalMissionAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextArtifactSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.NoteDocumentItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.NoteDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.ScriptSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextConfigurationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextRoleProfileItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextRoleProfileSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextStructureItemSnapshot


/**
 * The single, versioned contract for data export, import, and synchronization.
 * It aggregates all feature-specific snapshots into one bundle.
 */
data class SnapshotBundle(
    @SerializedName("snapshotVersion")
    val version: Int = 1,
    @SerializedName("exportedAt")
    val exportedAt: Long = System.currentTimeMillis(),
    val contexts: List<ContextSnapshot> = emptyList(),
    val goals: List<GoalSnapshot> = emptyList(),
    val backlogItems: List<BacklogItemSnapshot> = emptyList(),
    val backlogOrders: List<BacklogOrderSnapshot> = emptyList(),
    val notes: List<LegacyNoteSnapshot> = emptyList(),
    val documents: List<NoteDocumentSnapshot> = emptyList(),
    val documentItems: List<NoteDocumentItemSnapshot> = emptyList(),
    val checklists: List<ChecklistSnapshot> = emptyList(),
    val checklistItems: List<ChecklistItemSnapshot> = emptyList(),
    val artifacts: List<ContextArtifactSnapshot> = emptyList(),
    val scripts: List<ScriptSnapshot> = emptyList(),
    val attachments: List<AttachmentSnapshot> = emptyList(),
    val crossRefs: List<ContextAttachmentCrossRefSnapshot> = emptyList(),
    val inbox: List<InboxRecordSnapshot> = emptyList(),
    val logs: List<ContextLogSnapshot> = emptyList(),
    val systemApps: List<SystemAppSnapshot> = emptyList(),
    val activityRecords: List<ActivityRecordSnapshot> = emptyList(),
    val recentProjectEntries: List<RecentProjectEntrySnapshot> = emptyList(),
    val linkItemEntities: List<LinkItemEntitySnapshot> = emptyList(),
    val dayPlans: List<DayPlanSnapshot> = emptyList(),
    val dayTasks: List<DayTaskSnapshot> = emptyList(),
    val dailyMetrics: List<DailyMetricSnapshot> = emptyList(),
    val conversations: List<ConversationSnapshot> = emptyList(),
    val chatMessages: List<ChatMessageSnapshot> = emptyList(),
    val conversationFolders: List<ConversationFolderSnapshot> = emptyList(),
    val reminders: List<ReminderSnapshot> = emptyList(),
    val recurringTasks: List<RecurringTaskSnapshot> = emptyList(),
    val tacticalMissions: List<TacticalMissionSnapshot> = emptyList(),
    val tacticalMissionAttachments: List<TacticalMissionAttachmentCrossRefSnapshot> = emptyList(),
    val aiEvents: List<AiEventSnapshot> = emptyList(),
    val aiInsights: List<AiInsightSnapshot> = emptyList(),
    val lifeSystemStates: List<LifeSystemStateSnapshot> = emptyList(),
    val contextRoleProfiles: List<ContextRoleProfileSnapshot> = emptyList(),
    val contextRoleProfileItems: List<ContextRoleProfileItemSnapshot> = emptyList(),
    val contextConfigurations: List<ContextConfigurationSnapshot> = emptyList(),
    val projectStructureItems: List<ContextStructureItemSnapshot> = emptyList()
)