package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextInboxSortingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextKeyProblemsEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.FocusContextIntervalEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LifeSystemStateEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.entities.UserStateIntervalEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiEventEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiInsightEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ChatMessageEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationFolderEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.DayManagementRuntimeStateSnapshot
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMissionAttachmentCrossRef

data class DatabaseContent(
    @SerializedName(value = "goals", alternate = ["a"])
    val goals: List<Goal> = emptyList(),
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<Context> = emptyList(),
    @SerializedName(value = "listItems", alternate = ["backlogItems_legacy_c"])
    val backlogItems: List<BacklogItem> = emptyList(),
    @SerializedName(value = "backlogOrders", alternate = ["order"])
    val backlogOrders: List<BacklogOrder> = emptyList(),
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<LegacyNoteEntity> = emptyList(),
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<NoteDocumentEntity> = emptyList(),
    @SerializedName("musicNotes")
    val musicNotes: List<MusicNoteEntity> = emptyList(),
    @SerializedName(value = "checklists", alternate = ["g"])
    val checklists: List<ChecklistEntity> = emptyList(),
    @SerializedName(value = "checklistItems", alternate = ["checklistItems_legacy_h"])
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    @SerializedName(value = "activityRecords", alternate = ["i"])
    val activityRecords: List<ActivityRecord> = emptyList(),
    @SerializedName(value = "scripts")
    val scripts: List<ScriptEntity> = emptyList(),
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry> = emptyList(),
    @SerializedName(value = "linkItemEntities", alternate = ["k"])
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
    @SerializedName(value = "directionItems")
    val directionItems: List<DirectionItemEntity> = emptyList(),
    @SerializedName(value = "inboxRecords", alternate = ["l"])
    val inboxRecords: List<InboxRecord> = emptyList(),
    @SerializedName(value = "projectExecutionLogs", alternate = ["m"])
    val contextLogs: List<ContextLog> = emptyList(),
    @SerializedName(value = "attachments", alternate = ["attachment_items"])
    val attachments: List<AttachmentEntity> = emptyList(),
    @SerializedName(value = "contextAttachmentCrossRefs", alternate = ["project_attachment_links", "projectAttachmentCrossRefs"])
    val contextAttachmentCrossRefs: List<ContextAttachmentCrossRef> =
        emptyList(),
    // --- Extended Entities ---
    @SerializedName("dayPlans")
    val dayPlans: List<DayPlan> = emptyList(),
    @SerializedName("dayFocusItems")
    val dayFocusItems: List<DayFocusItem> = emptyList(),
    @SerializedName("dayTasks")
    val dayTasks: List<DayTask> = emptyList(),
    @SerializedName("dailyMetrics")
    val dailyMetrics: List<DailyMetric> = emptyList(),
    @SerializedName("conversations")
    val conversations: List<ConversationEntity> = emptyList(),
    @SerializedName("chatMessages")
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    @SerializedName("conversationFolders")
    val conversationFolders: List<ConversationFolderEntity> = emptyList(),
    @SerializedName("reminders")
    val reminders: List<Reminder> = emptyList(),
    @SerializedName("recurringTasks")
    val recurringTasks: List<RecurringTask> = emptyList(),
    @SerializedName("systemApps")
    val systemApps: List<SystemAppEntity> = emptyList(),
    @SerializedName("projectArtifacts")
    val contextArtifacts: List<ContextArtifact> = emptyList(),
    @SerializedName("tacticalMissions")
    val tacticalMissions: List<TacticalMission> = emptyList(),
    @SerializedName("tacticalMissionAttachments")
    val tacticalMissionAttachments: List<TacticalMissionAttachmentCrossRef> =
        emptyList(),
    @SerializedName("aiEvents")
    val aiEvents: List<AiEventEntity> = emptyList(),
    @SerializedName("aiInsights")
    val aiInsights: List<AiInsightEntity> = emptyList(),
    @SerializedName("lifeSystemStates")
    val lifeSystemStates: List<LifeSystemStateEntity> = emptyList(),
    @SerializedName("structurePresets")
    val contextRoleProfiles: List<ContextRoleProfile> = emptyList(),
    @SerializedName("structurePresetItems")
    val contextRoleProfileItems: List<ContextRoleProfileItem> = emptyList(),
    @SerializedName("projectStructures")
    val contextConfigurations: List<ContextConfiguration> = emptyList(),
    @SerializedName("projectStructureItems")
    val projectStructureItems: List<ContextStructureItem> = emptyList(),
    @SerializedName("contextInboxSortingRules")
    val contextInboxSortingRules: List<ContextInboxSortingEntity> = emptyList(),
    @SerializedName("contextKeyProblems")
    val contextKeyProblems: List<ContextKeyProblemsEntity> = emptyList(),
    @SerializedName("focusContextIntervals")
    val focusContextIntervals: List<FocusContextIntervalEntity> = emptyList(),
    @SerializedName("userStateIntervals")
    val userStateIntervals: List<UserStateIntervalEntity> = emptyList(),
    @SerializedName("dayManagementRuntimeState")
    val dayManagementRuntimeState: DayManagementRuntimeStateSnapshot? = null,
)
