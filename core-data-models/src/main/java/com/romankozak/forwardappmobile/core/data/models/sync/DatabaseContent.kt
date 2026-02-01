package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.ContextLog
import com.romankozak.forwardappmobile.core.data.models.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.LifeSystemStateEntity
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.Reminder
import com.romankozak.forwardappmobile.core.data.models.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.ai.AiEventEntity
import com.romankozak.forwardappmobile.core.data.models.ai.AiInsightEntity
import com.romankozak.forwardappmobile.core.data.models.ai.ChatMessageEntity
import com.romankozak.forwardappmobile.core.data.models.ai.ConversationEntity
import com.romankozak.forwardappmobile.core.data.models.ai.ConversationFolderEntity
import com.romankozak.forwardappmobile.core.data.models.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.tactical.TacticalMissionAttachmentCrossRef

data class DatabaseContent(
    @SerializedName(value = "goals", alternate = ["a"])
    val goals: List<Goal> = emptyList(),
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<Context> = emptyList(),
    @SerializedName(value = "listItems", alternate = ["c"])
    val backlogItems: List<BacklogItem> = emptyList(),
    @SerializedName(value = "backlogOrders", alternate = ["order"])
    val backlogOrders: List<BacklogOrder> = emptyList(),
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<LegacyNoteEntity> = emptyList(),
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<NoteDocumentEntity> = emptyList(),
    @SerializedName(value = "checklists", alternate = ["g"])
    val checklists: List<ChecklistEntity> = emptyList(),
    @SerializedName(value = "checklistItems", alternate = ["h"])
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    @SerializedName(value = "activityRecords", alternate = ["i"])
    val activityRecords: List<ActivityRecord> = emptyList(),
    @SerializedName(value = "scripts")
    val scripts: List<ScriptEntity> = emptyList(),
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry> = emptyList(),
    @SerializedName(value = "linkItemEntities", alternate = ["k"])
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
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
)