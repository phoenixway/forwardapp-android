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

    @SerializedName("contexts") val contexts: List<ContextSnapshot> = emptyList(),
    @SerializedName("goals") val goals: List<GoalSnapshot> = emptyList(),
    @SerializedName("backlogItems") val backlogItems: List<BacklogItemSnapshot> = emptyList(),
    @SerializedName("backlogOrders") val backlogOrders: List<BacklogOrderSnapshot> = emptyList(),
    @SerializedName("notes") val notes: List<LegacyNoteSnapshot> = emptyList(),
    @SerializedName("documents") val documents: List<NoteDocumentSnapshot> = emptyList(),
    @SerializedName("checklists") val checklists: List<ChecklistSnapshot> = emptyList(),
    @SerializedName("checklistItems") val checklistItems: List<ChecklistItemSnapshot> = emptyList(),
    @SerializedName("artifacts") val artifacts: List<ContextArtifactSnapshot> = emptyList(),
    @SerializedName("scripts") val scripts: List<ScriptSnapshot> = emptyList(),
    @SerializedName("attachments") val attachments: List<AttachmentSnapshot> = emptyList(),
    @SerializedName("crossRefs") val crossRefs: List<ContextAttachmentCrossRefSnapshot> = emptyList(),
    @SerializedName("inbox") val inbox: List<InboxRecordSnapshot> = emptyList(),
    @SerializedName("logs") val logs: List<ContextLogSnapshot> = emptyList(),
    @SerializedName("systemApps") val systemApps: List<SystemAppSnapshot> = emptyList(),
    @SerializedName("activityRecords") val activityRecords: List<ActivityRecordSnapshot> = emptyList(),
    @SerializedName("recentProjectEntries") val recentProjectEntries: List<RecentProjectEntrySnapshot> = emptyList(),
    @SerializedName("linkItemEntities") val linkItemEntities: List<LinkItemEntitySnapshot> = emptyList(),
    @SerializedName("dayPlans") val dayPlans: List<DayPlanSnapshot> = emptyList(),
    @SerializedName("dayTasks") val dayTasks: List<DayTaskSnapshot> = emptyList(),
    @SerializedName("dailyMetrics") val dailyMetrics: List<DailyMetricSnapshot> = emptyList(),
    @SerializedName("conversations") val conversations: List<ConversationSnapshot> = emptyList(),
    @SerializedName("chatMessages") val chatMessages: List<ChatMessageSnapshot> = emptyList(),
    @SerializedName("conversationFolders") val conversationFolders: List<ConversationFolderSnapshot> = emptyList(),
    @SerializedName("reminders") val reminders: List<ReminderSnapshot> = emptyList(),
    @SerializedName("recurringTasks") val recurringTasks: List<RecurringTaskSnapshot> = emptyList(),
    @SerializedName("tacticalMissions") val tacticalMissions: List<TacticalMissionSnapshot> = emptyList(),
    @SerializedName("tacticalMissionAttachments") val tacticalMissionAttachments: List<TacticalMissionAttachmentCrossRefSnapshot> = emptyList(),
    @SerializedName("aiEvents") val aiEvents: List<AiEventSnapshot> = emptyList(),
    @SerializedName("aiInsights") val aiInsights: List<AiInsightSnapshot> = emptyList(),
    @SerializedName("lifeSystemStates") val lifeSystemStates: List<LifeSystemStateSnapshot> = emptyList(),
    @SerializedName("contextRoleProfiles") val contextRoleProfiles: List<ContextRoleProfileSnapshot> = emptyList(),
    @SerializedName("contextRoleProfileItems") val contextRoleProfileItems: List<ContextRoleProfileItemSnapshot> = emptyList(),
    @SerializedName("contextConfigurations") val contextConfigurations: List<ContextConfigurationSnapshot> = emptyList(),
    @SerializedName("projectStructureItems") val projectStructureItems: List<ContextStructureItemSnapshot> = emptyList(),
)