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
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.NoteDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.MusicNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ScriptSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogOrderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextConfigurationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextInboxSortingSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextStructureItemSnapshot
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
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SavedOrientationViewEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemAttachmentRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemWorkspaceRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot


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
    @SerializedName("notes") val notes: List<LegacyNoteSnapshot> = emptyList(),
    @SerializedName("documents") val documents: List<NoteDocumentSnapshot> = emptyList(),
    @SerializedName("musicNotes") val musicNotes: List<MusicNoteSnapshot> = emptyList(),
    @SerializedName("checklists") val checklists: List<ChecklistSnapshot> = emptyList(),
    @SerializedName("checklistItems") val checklistItems: List<ChecklistItemSnapshot> = emptyList(),
    @SerializedName("scripts") val scripts: List<ScriptSnapshot> = emptyList(),
    @SerializedName("attachments") val attachments: List<AttachmentSnapshot> = emptyList(),
    @SerializedName("crossRefs") val crossRefs: List<ContextAttachmentCrossRefSnapshot> = emptyList(),
    @SerializedName("inbox") val inbox: List<InboxRecordSnapshot> = emptyList(),
    @SerializedName("logs") val logs: List<ContextLogSnapshot> = emptyList(),

    // Canonical EXECUTION_LOG contract. null means absent/unsupported;
    // emptyList() means canonical authority is present and currently empty.
    @SerializedName("canonicalExecutionLogs")
    val canonicalExecutionLogs: List<CanonicalExecutionLogSnapshot>? = null,

    // Canonical DIRECTION ordered-placement contract.
    // null = absent/unsupported; [] = authoritative empty.
    @SerializedName("workspaceDirectionEntries")
    val workspaceDirectionEntries: List<WorkspaceDirectionEntrySnapshot>? = null,
    @SerializedName("workspaceConnections")
    val workspaceConnections: List<WorkspaceConnectionSnapshot>? = null,
    @SerializedName("workspaceProblems")
    val workspaceProblems: List<WorkspaceProblemSnapshot>? = null,
    @SerializedName("workspaceProblemWorkspaceRefs")
    val workspaceProblemWorkspaceRefs: List<WorkspaceProblemWorkspaceRefSnapshot>? = null,
    @SerializedName("workspaceProblemAttachmentRefs")
    val workspaceProblemAttachmentRefs: List<WorkspaceProblemAttachmentRefSnapshot>? = null,
    @SerializedName("workspaceInboxRecords")
    val workspaceInboxRecords: List<WorkspaceInboxRecordSnapshot>? = null,
    // Canonical BACKLOG ordered-placement contract.
    // null = absent/pre-cutover backup; [] = authoritative empty.
    @SerializedName("workspaceBacklogEntries")
    val workspaceBacklogEntries: List<WorkspaceBacklogEntrySnapshot>? = null,
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

    // Canonical Orientation contract. null means the whole contract is absent;
    // emptyList() means the corresponding authoritative collection is empty.
    @SerializedName("managedSubjects") val managedSubjects: List<ManagedSubjectEntity>? = null,
    @SerializedName("orientations") val orientations: List<OrientationEntity>? = null,
    @SerializedName("aspects") val aspects: List<AspectEntity>? = null,
    @SerializedName("orientationAssessments")
    val orientationAssessments: List<OrientationAssessmentEntity>? = null,
    @SerializedName("orientationAssessmentRevisions")
    val orientationAssessmentRevisions: List<OrientationAssessmentRevisionEntity>? = null,
    @SerializedName("legacySubjectMappings")
    val legacySubjectMappings: List<LegacySubjectMappingEntity>? = null,
    @SerializedName("orientationRelations")
    val orientationRelations: List<OrientationRelationEntity>? = null,
    @SerializedName("aspectOrientationRefs")
    val aspectOrientationRefs: List<AspectOrientationRefEntity>? = null,
    @SerializedName("workspaces") val workspaces: List<WorkspaceEntity>? = null,
    @SerializedName("workspaceBindings") val workspaceBindings: List<WorkspaceBindingEntity>? = null,
    @SerializedName("workspaceCapabilityInstances")
    val workspaceCapabilityInstances: List<WorkspaceCapabilityInstanceEntity>? = null,
    @SerializedName("savedOrientationViews")
    val savedOrientationViews: List<SavedOrientationViewEntity>? = null,

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
    @SerializedName("focusContextIntervals") val focusContextIntervals: List<FocusContextIntervalSnapshot> = emptyList(),
    @SerializedName("userStateIntervals") val userStateIntervals: List<UserStateIntervalSnapshot> = emptyList(),
)
