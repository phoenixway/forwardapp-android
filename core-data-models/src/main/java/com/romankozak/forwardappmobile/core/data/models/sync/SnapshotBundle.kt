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
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalActivitySlot
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextArtifactSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.NoteDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.MusicNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ScriptSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogOrderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextConfigurationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextInboxSortingSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextKeyProblemsSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextStructureItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.DirectionItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.GoalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.LinkItemEntitySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.SystemAppSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DailyMetricSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayFocusItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurringTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.DayManagementRuntimeStateSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.FocusContextIntervalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.LifeManagementLevelStatusSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.LifeSystemStateSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupMemberSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconLevelStatusSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.RecentProjectEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.UserStateIntervalSnapshot


/**
 * The single, versioned contract for data export, import, and synchronization.
 * It aggregates all feature-specific snapshots into one bundle.
 */
data class SnapshotBundle(
    @SerializedName(value = "snapshotVersion", alternate = ["version"])
    val version: Int = 1,

    @SerializedName("exportedAt")
    val exportedAt: Long = System.currentTimeMillis(),

    @SerializedName("contexts") val contexts: List<ContextSnapshot> = emptyList(),
    @SerializedName("contextParentLinks") val contextParentLinks: List<ContextParentLinkSnapshot> = emptyList(),
    @SerializedName("goals") val goals: List<GoalSnapshot> = emptyList(),
    @SerializedName("backlogItems") val backlogItems: List<BacklogItemSnapshot> = emptyList(),
    @SerializedName("backlogOrders") val backlogOrders: List<BacklogOrderSnapshot> = emptyList(),
    @SerializedName("directionItems") val directionItems: List<DirectionItemSnapshot> = emptyList(),
    @SerializedName("notes") val notes: List<LegacyNoteSnapshot> = emptyList(),
    @SerializedName("documents") val documents: List<NoteDocumentSnapshot> = emptyList(),
    @SerializedName("musicNotes") val musicNotes: List<MusicNoteSnapshot> = emptyList(),
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
    @SerializedName("dayFocusItems") val dayFocusItems: List<DayFocusItemSnapshot> = emptyList(),
    @SerializedName("dayTasks") val dayTasks: List<DayTaskSnapshot> = emptyList(),
    @SerializedName("dayThemeDocuments") val dayThemeDocuments: List<DayThemeDocumentSnapshot> = emptyList(),

    // Canonical Day Themes contract. null means the field was absent from the
    // wire payload; emptyList() means canonical authority is present and empty.
    @SerializedName("themeDefinitions") val themeDefinitions: List<ThemeDefinitionSnapshot>? = null,
    @SerializedName("dayThemes") val dayThemes: List<DayThemeSnapshot>? = null,
    @SerializedName("dayThemeAssignmentDocuments")
    val dayThemeAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>? = null,

    @SerializedName("dailyMetrics") val dailyMetrics: List<DailyMetricSnapshot> = emptyList(),
    @SerializedName("conversations") val conversations: List<ConversationSnapshot> = emptyList(),
    @SerializedName("chatMessages") val chatMessages: List<ChatMessageSnapshot> = emptyList(),
    @SerializedName("conversationFolders") val conversationFolders: List<ConversationFolderSnapshot> = emptyList(),
    @SerializedName("reminders") val reminders: List<ReminderSnapshot> = emptyList(),
    @SerializedName("recurringTasks") val recurringTasks: List<RecurringTaskSnapshot> = emptyList(),
    @SerializedName("recurringSeries") val recurringSeries: List<CanonicalRecurringSeriesSnapshot> = emptyList(),
    @SerializedName("tacticalMissions") val tacticalMissions: List<TacticalMissionSnapshot> = emptyList(),
    @SerializedName("tacticalMissionAttachments") val tacticalMissionAttachments: List<TacticalMissionAttachmentCrossRefSnapshot> = emptyList(),
    @SerializedName("tacticalIterations") val tacticalIterations: List<TacticalIteration> = emptyList(),
    @SerializedName("missionStreams") val missionStreams: List<MissionStream> = emptyList(),
    @SerializedName("tacticalActivitySlots") val tacticalActivitySlots: List<TacticalActivitySlot> = emptyList(),
    @SerializedName("arcQuests") val arcQuests: List<ArcQuestEntity> = emptyList(),
    @SerializedName("aiEvents") val aiEvents: List<AiEventSnapshot> = emptyList(),
    @SerializedName("aiInsights") val aiInsights: List<AiInsightSnapshot> = emptyList(),
    @SerializedName("mainBeacons") val mainBeacons: List<MainBeaconSnapshot> = emptyList(),
    @SerializedName("mainBeaconGroups") val mainBeaconGroups: List<MainBeaconGroupSnapshot> = emptyList(),
    @SerializedName("mainBeaconGroupMembers") val mainBeaconGroupMembers: List<MainBeaconGroupMemberSnapshot> = emptyList(),
    @SerializedName("mainBeaconParentLinks") val mainBeaconParentLinks: List<MainBeaconParentLinkSnapshot> = emptyList(),
    @SerializedName("mainBeaconContextCrossRefs") val mainBeaconContextCrossRefs: List<MainBeaconContextCrossRefSnapshot> = emptyList(),
    @SerializedName("mainBeaconAttachmentCrossRefs") val mainBeaconAttachmentCrossRefs: List<MainBeaconAttachmentCrossRefSnapshot> = emptyList(),
    @SerializedName("mainBeaconLevelStatuses") val mainBeaconLevelStatuses: List<MainBeaconLevelStatusSnapshot> = emptyList(),
    @SerializedName("lifeManagementLevelStatuses") val lifeManagementLevelStatuses: List<LifeManagementLevelStatusSnapshot> = emptyList(),
    @SerializedName("lifeSystemStates") val lifeSystemStates: List<LifeSystemStateSnapshot> = emptyList(),
    @SerializedName("dayManagementRuntimeState") val dayManagementRuntimeState: DayManagementRuntimeStateSnapshot? = null,
    @SerializedName("contextRoleProfiles") val contextRoleProfiles: List<ContextRoleProfileSnapshot> = emptyList(),
    @SerializedName("contextRoleProfileItems") val contextRoleProfileItems: List<ContextRoleProfileItemSnapshot> = emptyList(),
    @SerializedName("contextConfigurations") val contextConfigurations: List<ContextConfigurationSnapshot> = emptyList(),
    @SerializedName("projectStructureItems") val projectStructureItems: List<ContextStructureItemSnapshot> = emptyList(),
    @SerializedName("contextInboxSortingRules") val contextInboxSortingRules: List<ContextInboxSortingSnapshot> = emptyList(),
    @SerializedName("contextKeyProblems") val contextKeyProblems: List<ContextKeyProblemsSnapshot> = emptyList(),
    @SerializedName("focusContextIntervals") val focusContextIntervals: List<FocusContextIntervalSnapshot> = emptyList(),
    @SerializedName("userStateIntervals") val userStateIntervals: List<UserStateIntervalSnapshot> = emptyList(),
)
