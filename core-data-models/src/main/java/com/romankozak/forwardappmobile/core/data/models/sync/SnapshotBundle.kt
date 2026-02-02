package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.activity.ActivityRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.AiEventSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.AiInsightSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.ChatMessageSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.ConversationFolderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.ConversationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.AttachmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.reminders.ReminderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextArtifactSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.NoteDocumentSnapshot
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
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.SystemAppSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DailyMetricSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurringTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.LifeSystemStateSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.RecentProjectEntrySnapshot


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